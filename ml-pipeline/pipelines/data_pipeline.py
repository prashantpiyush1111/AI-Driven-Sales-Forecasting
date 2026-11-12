"""
Data Cleaning & Feature Engineering Pipeline
=============================================
Dataset : Walmart M5 Sales Forecasting
Purpose : Clean raw CSV files, melt sales into time-series format,
          merge calendar & price features, compute lag & rolling features,
          and save the clean dataset to the Feature Store.
"""

import os
import gc
import logging
from pathlib import Path
import numpy as np
import pandas as pd

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s"
)
logger = logging.getLogger(__name__)


# STEP 1: Dynamic Path Resolution
def get_paths():
    """Finds raw data folder and feature-store directory dynamically."""
    script_dir = Path(__file__).resolve().parent
    
    # Locate project root (look for marker files/directories like feature-store or ml-pipeline)
    current = script_dir
    project_root = None
    while current.parent != current:
        if (current / "feature-store").exists() or (current / "ml-pipeline").exists():
            project_root = current
            break
        current = current.parent
    
    if project_root is None:
        project_root = script_dir.parents[1]

    # Check potential raw data locations
    candidates = [
        project_root / "m5-forecasting-accuracy (walmart data)",
        project_root / "m5_data",
        project_root / "data" / "raw",
        project_root / "data",
        script_dir.parents[1] / "m5-forecasting-accuracy (walmart data)",
        Path.cwd() / "m5-forecasting-accuracy (walmart data)",
    ]
    
    data_in = None
    for p in candidates:
        if p.exists() and (p / "calendar.csv").exists():
            data_in = p.resolve()
            break

    if not data_in:
        searched_paths = "\n - ".join(str(p.resolve()) for p in candidates)
        raise FileNotFoundError(
            f"Raw data directory containing 'calendar.csv' not found.\nSearched candidate locations:\n - {searched_paths}"
        )

    # Output directory in feature-store
    data_out = (project_root / "feature-store" / "data").resolve()
    data_out.mkdir(parents=True, exist_ok=True)

    return data_in, data_out


# STEP 2: Main Pipeline Function
def run_sales_pipeline(days_window: int = 100):
    """
    Runs end-to-end data cleaning & feature engineering.
    
    Args:
        days_window: Number of recent days to process (default: last 100 days)
                     to maintain ultra-fast execution and optimal memory usage.
    """
    data_in, data_out = get_paths()
    logger.info("=" * 65)
    logger.info("  AI-DRIVEN SALES FORECASTING - DATA PIPELINE")
    logger.info("=" * 65)
    logger.info(f"[*] Input Data Folder  : {data_in}")
    logger.info(f"[*] Output Data Folder : {data_out}")

    # 2.1 Load Raw CSV Data
    logger.info("[Step 1/6] Loading raw CSV files...")
    try:
        calendar = pd.read_csv(data_in / "calendar.csv")
    except Exception as e:
        raise FileNotFoundError(f"Failed to read 'calendar.csv' from {data_in}: {e}") from e

    try:
        prices = pd.read_csv(data_in / "sell_prices.csv")
    except Exception as e:
        raise FileNotFoundError(f"Failed to read 'sell_prices.csv' from {data_in}: {e}") from e

    sales_file = "sales_train_evaluation.csv" if (data_in / "sales_train_evaluation.csv").exists() else "sales_train_validation.csv"
    try:
        sales = pd.read_csv(data_in / sales_file)
    except Exception as e:
        raise FileNotFoundError(f"Failed to read sales file '{sales_file}' from {data_in}: {e}") from e

    logger.info(f"    Loaded {len(sales):,} products, {len(calendar):,} calendar dates, {len(prices):,} price entries.")

    # 2.2 Clean & Prepare Calendar Data
    logger.info("[Step 2/6] Cleaning Calendar & Extracting Date Features...")
    calendar["date"] = pd.to_datetime(calendar["date"])
    
    # Extract temporal indicators
    calendar["day"] = calendar["date"].dt.day.astype(np.int8)
    calendar["month"] = calendar["date"].dt.month.astype(np.int8)
    calendar["year"] = calendar["date"].dt.year.astype(np.int16)
    
    # Weekend indicator (Saturday & Sunday)
    calendar["is_weekend"] = calendar["date"].dt.dayofweek.isin([5, 6]).astype(np.int8)
    
    # Event handling
    calendar["has_event"] = (~calendar["event_name_1"].isnull()).astype(np.int8)
    
    # Optimize SNAP columns
    for snap_col in ["snap_CA", "snap_TX", "snap_WI"]:
        if snap_col in calendar.columns:
            calendar[snap_col] = calendar[snap_col].fillna(0).astype(np.int8)
        else:
            calendar[snap_col] = 0

    # Keep only essential calendar columns
    selected_calendar_cols = [
        "d", "date", "wm_yr_wk", "wday", "day", "month", "year", 
        "is_weekend", "has_event", "snap_CA", "snap_TX", "snap_WI"
    ]
    calendar_clean = calendar[[col for col in selected_calendar_cols if col in calendar.columns]].copy()
    del calendar
    gc.collect()

    # 2.3 Reshape (Melt) Sales: Wide -> Time-Series Long Format
    d_cols_all = [c for c in sales.columns if c.startswith("d_") and c[2:].isdigit()]
    if not d_cols_all:
        raise ValueError("No sales day columns (e.g., 'd_1', 'd_2', ...) found in sales file.")
    
    d_numbers = sorted([int(c.split("_")[1]) for c in d_cols_all])
    max_day = d_numbers[-1]
    start_day = max(d_numbers[0], max_day - days_window + 1)
    day_cols = [f"d_{i}" for i in range(start_day, max_day + 1) if f"d_{i}" in sales.columns]
    
    logger.info(f"[Step 3/6] Reshaping sales data (Last {len(day_cols)} days: d_{start_day} to d_{max_day})...")
    id_cols = [c for c in ["id", "item_id", "dept_id", "cat_id", "store_id", "state_id"] if c in sales.columns]

    sales_melted = pd.melt(
        sales[id_cols + day_cols],
        id_vars=id_cols,
        value_vars=day_cols,
        var_name="d",
        value_name="sales"
    )

    # Free memory from wide raw table
    del sales
    gc.collect()

    # Clean sales column
    sales_melted["sales"] = sales_melted["sales"].fillna(0)
    sales_melted["sales"] = np.maximum(0, sales_melted["sales"]).astype(np.int16)

    # Convert object strings to categorical for fast querying and small memory
    for col in id_cols:
        sales_melted[col] = sales_melted[col].astype("category")

    # 2.4 Merge Sales with Calendar and Sell Prices
    logger.info("[Step 4/6] Merging Sales, Calendar, and Prices...")

    # Merge Calendar on 'd'
    merged = pd.merge(sales_melted, calendar_clean, on="d", how="left")
    del sales_melted, calendar_clean
    gc.collect()

    # Optimize Price columns
    prices["store_id"] = prices["store_id"].astype("category")
    prices["item_id"] = prices["item_id"].astype("category")
    prices["sell_price"] = prices["sell_price"].astype(np.float32)

    # Merge Prices on ['store_id', 'item_id', 'wm_yr_wk']
    merged = pd.merge(merged, prices, on=["store_id", "item_id", "wm_yr_wk"], how="left")
    del prices
    gc.collect()

    # Impute missing prices with 0.0
    merged["sell_price"] = merged["sell_price"].fillna(0.0).astype(np.float32)

    # 2.5 Feature Engineering (Revenue, Lags & Rolling Averages)
    logger.info("[Step 5/6] Generating ML Features (Revenue, Lag_7, Rolling_Mean_7)...")
    
    # 1. Revenue feature
    merged["revenue"] = (merged["sales"] * merged["sell_price"]).astype(np.float32)

    # 2. Time-series sorting by item & date
    merged = merged.sort_values(["id", "date"]).reset_index(drop=True)

    # 3. Lag_7 feature: Sales from 7 days ago (weekly seasonality)
    merged["lag_7_sales"] = (
        merged.groupby("id", observed=False)["sales"]
        .shift(7)
        .fillna(0.0)
        .astype(np.float32)
    )

    # 4. Rolling Mean 7: Average sales over past 7 days (trend feature)
    merged["rolling_mean_7"] = (
        merged.groupby("id", observed=False)["sales"]
        .transform(lambda x: x.shift(1).rolling(window=7, min_periods=1).mean())
        .fillna(0.0)
        .astype(np.float32)
    )

    # 2.6 Save to Feature Store
    logger.info("[Step 6/6] Saving Processed Data to Feature Store...")
    
    parquet_path = data_out / "processed_m5_sales.parquet"
    try:
        merged.to_parquet(parquet_path, index=False)
        logger.info(f"    [SUCCESS] Saved Parquet: {parquet_path}")
    except Exception as e:
        csv_path = data_out / "processed_m5_sales.csv"
        merged.to_csv(csv_path, index=False)
        logger.info(f"    [SUCCESS] Saved CSV: {csv_path} (Reason: {e})")

    logger.info("=" * 65)
    logger.info(f"  PIPELINE COMPLETE! Total Clean Records: {len(merged):,}")
    logger.info(f"  Memory Footprint: {merged.memory_usage().sum() / 1024**2:.2f} MB")
    logger.info("=" * 65)

    return merged


# Main Execution Entrypoint
if __name__ == "__main__":
    df_clean = run_sales_pipeline(days_window=100)

    print("\n--- SAMPLE CLEANED DATA (First 10 Rows) ---")
    preview_cols = [
        "id", "date", "sales", "sell_price", "revenue", 
        "lag_7_sales", "rolling_mean_7", "is_weekend", "snap_CA"
    ]
    avail_cols = [c for c in preview_cols if c in df_clean.columns]
    print(df_clean[avail_cols].head(10).to_string())