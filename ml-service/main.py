"""
FastAPI ML Inference Service — Sales Forecasting
=================================================
Backend API Contract:
  POST /predict
    Request:  {"product_name": str, "forecast_date": str (YYYY-MM-DD)}
    Response: {
      "product_name": str,
      "forecast_date": str,
      "predicted_quantity": int,
      "predicted_revenue": float,
      "confidence_score": float,
      "model_version": str
    }
"""

import json
import logging
from pathlib import Path
from typing import Any, Dict, Optional, Tuple

import joblib
import lightgbm as lgb
import numpy as np
import pandas as pd
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("ml_service")


# =====================================================================
# Global Cache for Model, Metadata & Feature Lookup
# =====================================================================
class ModelContext:
    model: Optional[lgb.Booster] = None
    metadata: Dict[str, Any] = {}
    lookup_df: Optional[pd.DataFrame] = None
    feature_cols: list = []
    cat_cols: list = []
    test_mae: float = 1.0272


ctx = ModelContext()


def get_base_dirs() -> Tuple[Path, Path]:
    """Locates models dir and feature store data dir dynamically."""
    current = Path(__file__).resolve().parent
    project_root = current
    while project_root.parent != project_root:
        if (project_root / "feature-store").exists() or (project_root / "ml-pipeline").exists():
            break
        project_root = project_root.parent

    models_dir = current / "models"
    feature_data = project_root / "feature-store" / "data"
    return models_dir, feature_data


def load_artifacts():
    """Synchronous loader for model, metadata, and fast feature lookup cache."""
    models_dir, feature_data = get_base_dirs()
    model_path = models_dir / "lightgbm_sales_model.joblib"
    meta_path = models_dir / "model_metadata.json"
    parquet_path = feature_data / "processed_m5_sales.parquet"

    # 1. Load Model
    if model_path.exists():
        logger.info(f"[*] Loading model from {model_path} ...")
        ctx.model = joblib.load(model_path)
        logger.info("[+] Model loaded successfully.")
    else:
        logger.warning(f"[!] Model file not found at {model_path}")

    # 2. Load Metadata
    if meta_path.exists():
        with open(meta_path, "r") as f:
            ctx.metadata = json.load(f)
        ctx.feature_cols = ctx.metadata.get("feature_columns", [])
        ctx.cat_cols = ctx.metadata.get("categorical_cols", [])
        ctx.test_mae = float(ctx.metadata.get("evaluation_metrics", {}).get("MAE", 1.0272))
        logger.info(f"[+] Loaded metadata (Test MAE: {ctx.test_mae:.4f})")

    # 3. Build in-memory lookup cache (latest known values per product)
    if parquet_path.exists():
        try:
            logger.info(f"[*] Caching latest product features from {parquet_path} ...")
            cols_to_read = [
                "id", "item_id", "dept_id", "cat_id",
                "store_id", "state_id", "sell_price",
                "lag_7_sales", "rolling_mean_7"
            ]
            df_full = pd.read_parquet(parquet_path, columns=cols_to_read)
            ctx.lookup_df = df_full.drop_duplicates(subset=["id"], keep="last").set_index("id")
            logger.info(f"[+] Cached features for {len(ctx.lookup_df):,} products.")
        except Exception as e:
            logger.warning(f"[!] Feature lookup cache failed to initialize: {e}")


# Initialize artifacts at startup
load_artifacts()

# =====================================================================
# FastAPI Application
# =====================================================================
app = FastAPI(
    title="AI-Driven Sales Forecasting — ML Service",
    description="Inference service for sales prediction matching backend Forecast contract",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# =====================================================================
# Pydantic Schemas (Backend API Contract)
# =====================================================================
class PredictRequest(BaseModel):
    product_name: str = Field(..., example="HOBBIES_1_001_CA_1")
    forecast_date: str = Field(..., example="2016-05-23")


class PredictResponse(BaseModel):
    product_name: str
    forecast_date: str
    predicted_quantity: int
    predicted_revenue: float
    confidence_score: float
    model_version: str


# =====================================================================
# Helper: Feature Builder
# =====================================================================
def build_feature_vector(product_name: str, forecast_date: str) -> Tuple[pd.DataFrame, float]:
    """
    Constructs the 18-feature row matching the LightGBM training matrix.
    Returns (DataFrame with 1 row, sell_price).
    """
    date_obj = pd.to_datetime(forecast_date)

    # Defaults
    item_id = "HOBBIES_1_001"
    store_id = "CA_1"
    dept_id = "HOBBIES_1"
    cat_id = "HOBBIES"
    state_id = "CA"
    sell_price = 4.99
    lag_7 = 1.0
    rolling_7 = 1.5

    # Check lookup cache
    if ctx.lookup_df is not None:
        matched_row = None
        candidates = [
            product_name,
            f"{product_name}_validation",
            f"{product_name}_evaluation",
        ]
        for cand in candidates:
            if cand in ctx.lookup_df.index:
                matched_row = ctx.lookup_df.loc[cand]
                break

        if matched_row is not None:
            item_id = str(matched_row["item_id"])
            store_id = str(matched_row["store_id"])
            dept_id = str(matched_row["dept_id"])
            cat_id = str(matched_row["cat_id"])
            state_id = str(matched_row["state_id"])
            sell_price = float(matched_row["sell_price"])
            lag_7 = float(matched_row["lag_7_sales"])
            rolling_7 = float(matched_row["rolling_mean_7"])
        else:
            tokens = product_name.split("_")
            if len(tokens) >= 5:
                item_id = f"{tokens[0]}_{tokens[1]}_{tokens[2]}"
                store_id = f"{tokens[3]}_{tokens[4]}"
                dept_id = f"{tokens[0]}_{tokens[1]}"
                cat_id = tokens[0]
                state_id = tokens[3]

    day = date_obj.day
    month = date_obj.month
    year = date_obj.year
    wday = (date_obj.dayofweek + 2) % 7 + 1   # Walmart M5: 1=Saturday .. 7=Friday
    is_weekend = 1 if date_obj.dayofweek in [5, 6] else 0
    wm_yr_wk = int(f"{year - 2000}{date_obj.isocalendar().week:02d}")
    has_event = 0
    snap_CA = 0
    snap_TX = 0
    snap_WI = 0

    row = {
        "item_id": item_id,
        "dept_id": dept_id,
        "cat_id": cat_id,
        "store_id": store_id,
        "state_id": state_id,
        "wm_yr_wk": wm_yr_wk,
        "wday": wday,
        "day": day,
        "month": month,
        "year": year,
        "is_weekend": is_weekend,
        "has_event": has_event,
        "snap_CA": snap_CA,
        "snap_TX": snap_TX,
        "snap_WI": snap_WI,
        "sell_price": np.float32(sell_price),
        "lag_7_sales": np.float32(lag_7),
        "rolling_mean_7": np.float32(rolling_7),
    }

    feature_df = pd.DataFrame([row])

    for c in ctx.cat_cols:
        if c in feature_df.columns:
            feature_df[c] = feature_df[c].astype("category")

    return feature_df[ctx.feature_cols], sell_price


# =====================================================================
# API Endpoints
# =====================================================================
@app.get("/health")
def health():
    """Health check endpoint for Docker / Kubernetes / Spring Boot."""
    return {
        "status": "healthy",
        "model_loaded": ctx.model is not None,
        "model_version": ctx.metadata.get("model_name", "LightGBM-v1"),
        "cached_products": len(ctx.lookup_df) if ctx.lookup_df is not None else 0,
    }


@app.get("/model/metadata")
def get_metadata():
    """Returns training metadata, hyperparameters, and holdout test metrics."""
    if not ctx.metadata:
        raise HTTPException(status_code=404, detail="Model metadata not found")
    return ctx.metadata


@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest):
    """
    Main inference endpoint matching backend Forecast.java entity contract.
    """
    if ctx.model is None:
        raise HTTPException(status_code=503, detail="Model is not loaded.")

    try:
        X, sell_price = build_feature_vector(req.product_name, req.forecast_date)

        raw_pred = ctx.model.predict(X, num_iteration=ctx.model.best_iteration)[0]
        predicted_quantity = max(0, int(round(float(raw_pred))))
        predicted_revenue = round(float(predicted_quantity * sell_price), 2)

        # Confidence score derived from test MAE and predicted quantity
        relative_error = ctx.test_mae / (predicted_quantity + 1.0)
        confidence = 1.0 / (1.0 + relative_error * 0.5)
        confidence_score = round(float(min(0.98, max(0.50, confidence))), 2)

        version = ctx.metadata.get("model_name", "LightGBM-v1.0")

        return PredictResponse(
            product_name=req.product_name,
            forecast_date=req.forecast_date,
            predicted_quantity=predicted_quantity,
            predicted_revenue=predicted_revenue,
            confidence_score=confidence_score,
            model_version=version,
        )
    except Exception as e:
        logger.error(f"Prediction failed for {req.product_name}: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Inference error: {str(e)}")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
