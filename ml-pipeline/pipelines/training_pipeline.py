"""
ML Training Pipeline (Part 1: Data Preparation & Splitting)
===========================================================
Dataset : Walmart M5 Sales Forecasting
Purpose : Load processed data from Feature Store, perform time-series 
          aware train/validation splitting, encode categorical features,
          and prepare training matrices for LightGBM.
"""

import os
import gc
import logging
from pathlib import Path
from typing import Tuple, List
import numpy as np
import pandas as pd
import joblib

# Setup Logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s"
)
logger = logging.getLogger(__name__)


# =====================================================================
# STEP 1: Dynamic Path Resolution
# =====================================================================
def get_pipeline_paths() -> Tuple[Path, Path]:
    """
    Locates processed data directory in Feature Store and sets up the Model output directory.
    """
    script_dir = Path(__file__).resolve().parent
    
    # Locate project root dynamically
    current = script_dir
    project_root = None
    while current.parent != current:
        if (current / "feature-store").exists() or (current / "ml-service").exists():
            project_root = current
            break
        current = current.parent
    
    if project_root is None:
        project_root = script_dir.parents[1]

    # Feature Store path
    feature_store_path = project_root / "feature-store" / "data"
    
    # Model Artifacts path (ml-service/models)
    models_dir = project_root / "ml-service" / "models"
    models_dir.mkdir(parents=True, exist_ok=True)
    
    return feature_store_path, models_dir


# =====================================================================
# STEP 2: Load Processed Data from Feature Store
# =====================================================================
def load_processed_data(feature_store_path: Path) -> pd.DataFrame:
    """
    Loads parquet or CSV dataset from Feature Store.
    """
    parquet_file = feature_store_path / "processed_m5_sales.parquet"
    csv_file = feature_store_path / "processed_m5_sales.csv"

    if parquet_file.exists():
        logger.info(f"[*] Loading processed data from Parquet: {parquet_file}")
        df = pd.read_parquet(parquet_file)
    elif csv_file.exists():
        logger.info(f"[*] Loading processed data from CSV: {csv_file}")
        df = pd.read_csv(csv_file)
    else:
        raise FileNotFoundError(
            f"No processed dataset found in {feature_store_path}. "
            "Please run 'data_pipeline.py' first."
        )

    logger.info(f"[+] Loaded {len(df):,} records with {len(df.columns)} columns.")
    return df


# =====================================================================
# STEP 3: Time-Series Train / Validation Split
# =====================================================================
def split_time_series(
    df: pd.DataFrame, 
    val_days: int = 28
) -> Tuple[pd.DataFrame, pd.DataFrame]:
    """
    Performs time-series aware splitting (prevents data leakage).
    Uses the last `val_days` (default: 28 days) as the validation set.
    """
    logger.info(f"[*] Splitting dataset into Train and Validation (Last {val_days} days for validation)...")
    
    if "date" not in df.columns:
        raise KeyError("'date' column is required for time-series splitting.")

    df["date"] = pd.to_datetime(df["date"])
    max_date = df["date"].max()
    split_date = max_date - pd.Timedelta(days=val_days)

    train_df = df[df["date"] <= split_date].copy()
    val_df = df[df["date"] > split_date].copy()

    logger.info(f"    - Training Period   : {train_df['date'].min().date()} to {train_df['date'].max().date()} ({len(train_df):,} rows)")
    logger.info(f"    - Validation Period : {val_df['date'].min().date()} to {val_df['date'].max().date()} ({len(val_df):,} rows)")

    return train_df, val_df


# =====================================================================
# STEP 4: Feature Matrix & Target Preparation
# =====================================================================
def prepare_features_and_target(
    train_df: pd.DataFrame, 
    val_df: pd.DataFrame
) -> Tuple[pd.DataFrame, pd.Series, pd.DataFrame, pd.Series, List[str]]:
    """
    Separates target ('sales') from feature matrix and encodes categorical columns.
    Returns: X_train, y_train, X_val, y_val, categorical_features
    """
    logger.info("[*] Preparing Feature matrices (X, y) and identifying categorical columns...")
    
    target_col = "sales"
    ignore_cols = ["id", "date", "d", "revenue", target_col]
    
    # Feature columns list
    feature_cols = [c for c in train_df.columns if c not in ignore_cols]
    
    # Categorical columns
    cat_cols = [c for c in ["item_id", "dept_id", "cat_id", "store_id", "state_id"] if c in feature_cols]
    
    # Ensure categories are encoded consistently as category dtype for LightGBM
    for col in cat_cols:
        train_df[col] = train_df[col].astype("category")
        val_df[col] = val_df[col].astype("category")

    X_train = train_df[feature_cols]
    y_train = train_df[target_col]

    X_val = val_df[feature_cols]
    y_val = val_df[target_col]

    logger.info(f"    - Features count : {len(feature_cols)} -> {feature_cols}")
    logger.info(f"    - Categoricals   : {cat_cols}")
    logger.info(f"    - Target variable: '{target_col}'")

    return X_train, y_train, X_val, y_val, cat_cols


# =====================================================================
# Preview / Dry Run Execution
# =====================================================================
if __name__ == "__main__":
    feature_store_path, models_dir = get_pipeline_paths()
    df = load_processed_data(feature_store_path)
    train_df, val_df = split_time_series(df, val_days=28)
    X_train, y_train, X_val, y_val, cat_cols = prepare_features_and_target(train_df, val_df)
    
    print("\n--- DATA SPLIT SUMMARY ---")
    print(f"X_train shape: {X_train.shape}, y_train shape: {y_train.shape}")
    print(f"X_val shape  : {X_val.shape}, y_val shape  : {y_val.shape}")
