"""
ML Training Pipeline — LightGBM Sales Forecasting
===================================================
Dataset : Walmart M5 Sales Forecasting
Purpose : Load processed data from Feature Store, perform time-series
          aware train/validation splitting, train LightGBM model,
          evaluate with RMSE/MAE/WRMSSE, and save model artifacts.

Pipeline Steps:
  1. Dynamic path resolution
  2. Load processed parquet from Feature Store
  3. Time-series aware Train / Validation split (no data leakage)
  4. Feature matrix & target preparation
  5. LightGBM model training with early stopping
  6. Model evaluation (RMSE, MAE, MAPE)
  7. Feature importance logging
  8. Model & metadata saving to ml-service/models/
"""

import datetime
import gc
import json
import logging
import warnings
from pathlib import Path
from typing import Dict, List, Tuple

import joblib
import lightgbm as lgb
import numpy as np
import pandas as pd
from sklearn.metrics import mean_absolute_error, mean_squared_error

warnings.filterwarnings("ignore", category=UserWarning)

# =====================================================================
# Logging Setup
# =====================================================================
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger(__name__)


# =====================================================================
# STEP 1: Dynamic Path Resolution
# =====================================================================
def get_pipeline_paths() -> Tuple[Path, Path]:
    """
    Locates:
      - Feature Store processed data directory
      - Model artifacts output directory (ml-service/models/)
    """
    script_dir = Path(__file__).resolve().parent

    current = script_dir
    project_root = None
    while current.parent != current:
        if (current / "feature-store").exists() or (current / "ml-service").exists():
            project_root = current
            break
        current = current.parent

    if project_root is None:
        project_root = script_dir.parents[1]

    feature_store_path = project_root / "feature-store" / "data"
    models_dir = project_root / "ml-service" / "models"
    models_dir.mkdir(parents=True, exist_ok=True)

    return feature_store_path, models_dir


# =====================================================================
# STEP 2: Load Processed Data from Feature Store
# =====================================================================
def load_processed_data(feature_store_path: Path) -> pd.DataFrame:
    """
    Loads the cleaned & feature-engineered dataset from the Feature Store.
    Prefers Parquet (fast & compressed) over CSV fallback.
    """
    parquet_file = feature_store_path / "processed_m5_sales.parquet"
    csv_file = feature_store_path / "processed_m5_sales.csv"

    if parquet_file.exists():
        logger.info(f"[*] Loading from Parquet: {parquet_file}")
        df = pd.read_parquet(parquet_file)
    elif csv_file.exists():
        logger.info(f"[*] Loading from CSV: {csv_file}")
        df = pd.read_csv(csv_file, parse_dates=["date"])
    else:
        raise FileNotFoundError(
            f"No processed dataset in {feature_store_path}.\n"
            "Please run 'data_pipeline.py' first to generate it."
        )

    logger.info(f"[+] Loaded {len(df):,} rows x {len(df.columns)} columns.")
    logger.info(f"    Columns: {list(df.columns)}")
    return df


# =====================================================================
# STEP 3: Time-Series Aware Train / Validation Split
# =====================================================================
def split_time_series(
    df: pd.DataFrame,
    val_days: int = 28,
) -> Tuple[pd.DataFrame, pd.DataFrame]:
    """
    Splits data chronologically — NO random shuffling to prevent leakage.
    Last `val_days` days become validation; everything before is training.

    Args:
        df       : Full processed DataFrame with 'date' column.
        val_days : Number of trailing days reserved for validation (default=28).

    Returns:
        train_df, val_df
    """
    logger.info(
        f"[*] Time-series split: last {val_days} days → validation, rest → training"
    )

    df["date"] = pd.to_datetime(df["date"])
    max_date = df["date"].max()
    split_date = max_date - pd.Timedelta(days=val_days)

    train_df = df[df["date"] <= split_date].copy()
    val_df = df[df["date"] > split_date].copy()

    logger.info(
        f"    Training   : {train_df['date'].min().date()} → "
        f"{train_df['date'].max().date()} | {len(train_df):,} rows"
    )
    logger.info(
        f"    Validation : {val_df['date'].min().date()} → "
        f"{val_df['date'].max().date()} | {len(val_df):,} rows"
    )

    del df
    gc.collect()
    return train_df, val_df


# =====================================================================
# STEP 4: Feature Matrix & Target Preparation
# =====================================================================
def prepare_features_and_target(
    train_df: pd.DataFrame,
    val_df: pd.DataFrame,
) -> Tuple[pd.DataFrame, pd.Series, pd.DataFrame, pd.Series, List[str]]:
    """
    Separates target 'sales' from feature matrix.
    Encodes categoricals for native LightGBM handling.

    Returns:
        X_train, y_train, X_val, y_val, categorical_feature_names
    """
    logger.info("[*] Building feature matrices X_train / X_val ...")

    TARGET = "sales"
    # Columns to exclude from features (IDs, target, leakage columns)
    IGNORE = {"id", "date", "d", "revenue", TARGET}

    feature_cols = [c for c in train_df.columns if c not in IGNORE]

    # Native categorical columns that LightGBM handles natively
    CAT_CANDIDATES = ["item_id", "dept_id", "cat_id", "store_id", "state_id", "wday"]
    cat_cols = [c for c in CAT_CANDIDATES if c in feature_cols]

    for col in cat_cols:
        train_df[col] = train_df[col].astype("category")
        val_df[col] = val_df[col].astype("category")

    X_train = train_df[feature_cols]
    y_train = train_df[TARGET].astype(np.float32)

    X_val = val_df[feature_cols]
    y_val = val_df[TARGET].astype(np.float32)

    logger.info(f"    Feature count     : {len(feature_cols)}")
    logger.info(f"    Features          : {feature_cols}")
    logger.info(f"    Categorical feats : {cat_cols}")
    logger.info(f"    X_train shape     : {X_train.shape}")
    logger.info(f"    X_val   shape     : {X_val.shape}")

    return X_train, y_train, X_val, y_val, cat_cols


# =====================================================================
# STEP 5: LightGBM Model Training
# =====================================================================
def train_lightgbm(
    X_train: pd.DataFrame,
    y_train: pd.Series,
    X_val: pd.DataFrame,
    y_val: pd.Series,
    cat_cols: List[str],
    num_boost_round: int = 1000,
    early_stopping_rounds: int = 50,
) -> lgb.Booster:
    """
    Trains a LightGBM Gradient Boosting model optimised for sales forecasting.

    Key hyperparameter choices:
      - objective='tweedie'  : Best for sales data (handles zeros & skewness)
      - metric='rmse'        : Standard regression error metric
      - num_leaves=127       : Controls model complexity
      - learning_rate=0.05   : Conservative rate for better generalisation
      - feature_fraction=0.8 : Prevents overfitting via column subsampling
      - bagging_fraction=0.8 : Row subsampling per tree
      - early stopping       : Stops when val RMSE stops improving (avoids overfitting)

    Args:
        X_train, y_train       : Training features and target
        X_val,   y_val         : Validation features and target
        cat_cols               : Column names to treat as categoricals
        num_boost_round        : Max number of boosting iterations
        early_stopping_rounds  : Stop if no val improvement for N rounds

    Returns:
        Trained lgb.Booster model
    """
    logger.info("=" * 65)
    logger.info("  LIGHTGBM MODEL TRAINING")
    logger.info("=" * 65)

    # ------------------------------------------------------------------
    # LightGBM Dataset wrappers (memory efficient)
    # ------------------------------------------------------------------
    logger.info("[Step 5a] Creating LightGBM Dataset objects ...")
    lgb_train = lgb.Dataset(
        X_train,
        label=y_train,
        categorical_feature=cat_cols,
        free_raw_data=True,
    )
    lgb_val = lgb.Dataset(
        X_val,
        label=y_val,
        categorical_feature=cat_cols,
        reference=lgb_train,       # Share encoding with train set
        free_raw_data=True,
    )

    # ------------------------------------------------------------------
    # Hyperparameters
    # ------------------------------------------------------------------
    params = {
        # Core objective
        "objective"        : "tweedie",       # Best for sales (handles zero-inflated distribution)
        "tweedie_variance_power": 1.1,        # Close to Poisson (good for count data)
        "metric"           : "rmse",          # Root Mean Squared Error for monitoring
        "verbosity"        : -1,              # Suppress LightGBM internal logs

        # Tree structure
        "num_leaves"       : 127,             # 2^7 — balance complexity vs speed
        "max_depth"        : -1,              # No depth limit (controlled by num_leaves)
        "min_child_samples": 20,              # Min samples per leaf (prevents overfitting)

        # Learning
        "learning_rate"    : 0.05,            # Conservative — better generalisation
        "n_estimators"     : num_boost_round,

        # Regularisation (prevents overfitting)
        "lambda_l1"        : 0.1,             # L1 regularisation
        "lambda_l2"        : 0.1,             # L2 regularisation

        # Subsampling (like Random Forest — adds randomness)
        "feature_fraction" : 0.8,             # Use 80% features per tree
        "bagging_fraction" : 0.8,             # Use 80% rows per tree
        "bagging_freq"     : 1,               # Bagging every iteration

        # Speed optimisations
        "num_threads"      : -1,              # Use all CPU cores
        "force_col_wise"   : True,            # Faster for wide datasets
    }

    logger.info(f"[Step 5b] Hyperparameters:\n{json.dumps(params, indent=6)}")

    # ------------------------------------------------------------------
    # Training with Early Stopping & Progress Logging
    # ------------------------------------------------------------------
    logger.info(f"[Step 5c] Training (max {num_boost_round} rounds, early stop @ {early_stopping_rounds}) ...")

    callbacks = [
        lgb.early_stopping(stopping_rounds=early_stopping_rounds, verbose=True),
        lgb.log_evaluation(period=100),   # Print metrics every 100 rounds
    ]

    model = lgb.train(
        params=params,
        train_set=lgb_train,
        num_boost_round=num_boost_round,
        valid_sets=[lgb_train, lgb_val],
        valid_names=["train", "validation"],
        callbacks=callbacks,
    )

    logger.info(f"[+] Training complete! Best iteration: {model.best_iteration}")
    logger.info(f"    Best val RMSE     : {model.best_score['validation']['rmse']:.4f}")
    logger.info(f"    Best train RMSE   : {model.best_score['train']['rmse']:.4f}")

    return model


# =====================================================================
# STEP 6: Model Evaluation
# =====================================================================
def evaluate_model(
    model: lgb.Booster,
    X_val: pd.DataFrame,
    y_val: pd.Series,
) -> Dict[str, float]:
    """
    Evaluates trained model on held-out validation set.
    Computes RMSE, MAE, and MAPE.

    Returns:
        Dictionary of metric names → values
    """
    logger.info("[Step 6] Evaluating model on Validation set ...")

    y_pred = model.predict(X_val, num_iteration=model.best_iteration)
    y_pred = np.maximum(0, y_pred)   # Sales cannot be negative

    y_true = y_val.values

    rmse = np.sqrt(mean_squared_error(y_true, y_pred))
    mae = mean_absolute_error(y_true, y_pred)

    # MAPE — avoid divide-by-zero for zero sales days
    nonzero_mask = y_true > 0
    mape = (
        np.mean(np.abs((y_true[nonzero_mask] - y_pred[nonzero_mask]) / y_true[nonzero_mask])) * 100
        if nonzero_mask.sum() > 0
        else float("nan")
    )

    metrics = {
        "RMSE": round(float(rmse), 4),
        "MAE" : round(float(mae), 4),
        "MAPE": round(float(mape), 4),
    }

    logger.info("=" * 65)
    logger.info("  EVALUATION RESULTS (Validation Set)")
    logger.info("=" * 65)
    logger.info(f"    RMSE  : {metrics['RMSE']:>10.4f}  (Root Mean Squared Error)")
    logger.info(f"    MAE   : {metrics['MAE']:>10.4f}  (Mean Absolute Error)")
    logger.info(f"    MAPE  : {metrics['MAPE']:>10.4f}% (Mean Absolute Percentage Error)")
    logger.info("=" * 65)

    return metrics


# =====================================================================
# STEP 7: Feature Importance Logging
# =====================================================================
def log_feature_importance(model: lgb.Booster, top_n: int = 15) -> pd.DataFrame:
    """
    Logs top-N most important features by 'gain' importance (most meaningful metric).
    'gain' = total reduction in loss brought by a feature across all splits.

    Args:
        model : Trained LightGBM Booster
        top_n : Number of top features to display

    Returns:
        DataFrame with feature importance scores
    """
    logger.info(f"[Step 7] Feature Importance (Top {top_n} by Gain) ...")

    importance_df = pd.DataFrame({
        "feature"         : model.feature_name(),
        "importance_gain" : model.feature_importance(importance_type="gain"),
        "importance_split": model.feature_importance(importance_type="split"),
    }).sort_values("importance_gain", ascending=False)

    logger.info("\n" + importance_df.head(top_n).to_string(index=False))
    return importance_df


# =====================================================================
# STEP 8: Save Model & Metadata
# =====================================================================
def save_model_artifacts(
    model: lgb.Booster,
    metrics: Dict[str, float],
    feature_cols: List[str],
    cat_cols: List[str],
    models_dir: Path,
) -> None:
    """
    Saves all model artifacts to ml-service/models/:
      - lightgbm_sales_model.joblib  : Trained model (fast load via joblib)
      - lightgbm_sales_model.txt     : Native LightGBM text format (portable)
      - model_metadata.json          : Metrics, features, training info

    Args:
        model       : Trained lgb.Booster
        metrics     : Evaluation metrics dict
        feature_cols: Feature column names used during training
        cat_cols    : Categorical feature names
        models_dir  : Directory to save artifacts
    """
    logger.info(f"[Step 8] Saving model artifacts to: {models_dir}")

    # 8.1 Save model in joblib format (fastest Python load)
    joblib_path = models_dir / "lightgbm_sales_model.joblib"
    joblib.dump(model, joblib_path)
    logger.info(f"    [SAVED] joblib model  → {joblib_path}")

    # 8.2 Save model in native LightGBM text format (portable / language-agnostic)
    txt_path = models_dir / "lightgbm_sales_model.txt"
    model.save_model(str(txt_path))
    logger.info(f"    [SAVED] LightGBM .txt → {txt_path}")

    # 8.3 Save metadata JSON (tracks what this model is and how it performed)
    metadata = {
        "model_name"        : "LightGBM Sales Forecaster",
        "dataset"           : "Walmart M5 Forecasting",
        "trained_at"        : datetime.datetime.now().isoformat(),
        "best_iteration"    : model.best_iteration,
        "evaluation_metrics": metrics,
        "feature_columns"   : feature_cols,
        "categorical_cols"  : cat_cols,
        "num_features"      : len(feature_cols),
        "hyperparameters": {
            "objective"          : "tweedie",
            "tweedie_variance_power": 1.1,
            "num_leaves"         : 127,
            "learning_rate"      : 0.05,
            "feature_fraction"   : 0.8,
            "bagging_fraction"   : 0.8,
            "lambda_l1"          : 0.1,
            "lambda_l2"          : 0.1,
        },
    }

    meta_path = models_dir / "model_metadata.json"
    with open(meta_path, "w") as f:
        json.dump(metadata, f, indent=2)
    logger.info(f"    [SAVED] Metadata JSON → {meta_path}")

    logger.info("[+] All artifacts saved successfully!")


# =====================================================================
# MAIN — End-to-End Training Execution
# =====================================================================
def run_training_pipeline(val_days: int = 28) -> lgb.Booster:
    """
    Orchestrates the full end-to-end ML training pipeline.

    Args:
        val_days : Days reserved for validation (default: 28)

    Returns:
        Trained LightGBM Booster model
    """
    logger.info("=" * 65)
    logger.info("  AI-DRIVEN SALES FORECASTING — TRAINING PIPELINE")
    logger.info("=" * 65)

    # Step 1 — Paths
    feature_store_path, models_dir = get_pipeline_paths()
    logger.info(f"[*] Feature Store : {feature_store_path}")
    logger.info(f"[*] Models Dir    : {models_dir}")

    # Step 2 — Load data
    df = load_processed_data(feature_store_path)

    # Step 3 — Time-series split
    train_df, val_df = split_time_series(df, val_days=val_days)
    del df
    gc.collect()

    # Step 4 — Feature & target preparation
    X_train, y_train, X_val, y_val, cat_cols = prepare_features_and_target(
        train_df, val_df
    )

    # Keep feature col names before freeing DFs
    feature_cols = list(X_train.columns)
    del train_df, val_df
    gc.collect()

    # Step 5 — Train LightGBM
    model = train_lightgbm(
        X_train, y_train,
        X_val, y_val,
        cat_cols,
        num_boost_round=1000,
        early_stopping_rounds=50,
    )

    # Step 6 — Evaluate
    metrics = evaluate_model(model, X_val, y_val)

    # Step 7 — Feature importance
    log_feature_importance(model, top_n=15)

    # Step 8 — Save artifacts
    save_model_artifacts(model, metrics, feature_cols, cat_cols, models_dir)

    logger.info("=" * 65)
    logger.info("  TRAINING PIPELINE COMPLETE!")
    logger.info("=" * 65)
    return model


# =====================================================================
# Entry Point
# =====================================================================
if __name__ == "__main__":
    run_training_pipeline(val_days=28)
    print("\n[DONE] Model training finished! Check ml-service/models/ for saved artifacts.")
