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
from pathlib import Path
import numpy as np
import pandas as pd


# STEP 1: Dynamic Path Resolution (Works everywhere)
def get_paths():
    """Finds raw data folder and feature-store directory dynamically."""
    script_dir = Path(__file__).resolve().parent
    project_root = script_dir.parents[1]  

    # Check potential raw data locations
    candidates = [
        project_root / "m5-forecasting-accuracy (walmart data)",
        script_dir.parents[2] / "m5-forecasting-accuracy (walmart data)",
        Path("../m5-forecasting-accuracy (walmart data)"),
    ]
    data_in = None
    for p in candidates:
        if p.exists() and (p / "calendar.csv").exists():
            data_in = p.resolve()
            break

    if not data_in:
        raise FileNotFoundError("Raw data directory 'm5-forecasting-accuracy (walmart data)' not found.")

    # Output directory in feature-store
    data_out = (project_root / "AI-Driven-Sales-Forecasting" / "feature-store" / "data").resolve()
    if not data_out.parent.exists():
        data_out = (script_dir.parents[1] / "feature-store" / "data").resolve()

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
    print("=" * 65)
    print("  AI-DRIVEN SALES FORECASTING - DATA PIPELINE")
    print("=" * 65)
    print(f"[*] Input Data Folder  : {data_in}")
    print(f"[*] Output Data Folder : {data_out}")


    # 2.1 Load Raw CSV Data
    print("\n[Step 1/6] Loading raw CSV files...")
    calendar = pd.read_csv(data_in / "calendar.csv")
    prices = pd.read_csv(data_in / "sell_prices.csv")
    
    sales_file = "sales_train_evaluation.csv" if (data_in / "sales_train_evaluation.csv").exists() else "sales_train_validation.csv"
    sales = pd.read_csv(data_in / sales_file)
    print(f"    Loaded {len(sales):,} products, {len(calendar):,} calendar dates, {len(prices):,} price entries.")

    # 2.2 Clean & Prepare Calendar Data
    print("\n[Step 2/6] Cleaning Calendar & Extracting Date Features...")
    calendar["date"] = pd.to_datetime(calendar["date"])
    
    # Extract temporal indicators
    calendar["day"] = calendar["date"].dt.day.astype(np.int8)
    calendar["month"] = calendar["date"].dt.month.astype(np.int8)
    calendar["year"] = calendar["date"].dt.year.astype(np.int16)
    calendar["is_weekend"] = calendar["wday"].isin([1, 2]).astype(np.int8)  # 1: Sat, 2: Sun
    
    # Event handling
    calendar["has_event"] = (~calendar["event_name_1"].isnull()).astype(np.int8)
    
    # Optimize SNAP columns
    for snap_col in ["snap_CA", "snap_TX", "snap_WI"]:
        calendar[snap_col] = calendar[snap_col].fillna(0).astype(np.int8)

    # Keep only essential calendar columns
    selected_calendar_cols = [
        "d", "date", "wm_yr_wk", "wday", "day", "month", "year", 
        "is_weekend", "has_event", "snap_CA", "snap_TX", "snap_WI"
    ]
    calendar_clean = calendar[selected_calendar_cols].copy()
    del calendar
    gc.collect()

    # 2.3 Reshape (Melt) Sales: Wide -> Time-Series Long Format
    print(f"\n[Step 3/6] Reshaping sales data (Last {days_window} days: d_{1942 - days_window} to d_1941)...")
    start_d = 1942 - days_window
    day_cols = [f"d_{i}" for i in range(start_d, 1942) if f"d_{i}" in sales.columns]
    id_cols = ["id", "item_id", "dept_id", "cat_id", "store_id", "state_id"]

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
    print("\n[Step 4/6] Merging Sales, Calendar, and Prices...")

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
    print("\n[Step 5/6] Generating ML Features (Revenue, Lag_7, Rolling_Mean_7)...")
    
    # 1. Revenue feature
    merged["revenue"] = (merged["sales"] * merged["sell_price"]).astype(np.float32)

    # 2. Time-series sorting by item & date
    merged = merged.sort_values(["id", "date"]).reset_index(drop=True)

    # 3. Lag_7 feature: Sales from 7 days ago (weekly seasonality)
    merged["lag_7_sales"] = merged.groupby("id", observed=False)["sales"].shift(7).astype(np.float32)

    # 4. Rolling Mean 7: Average sales over past 7 days (trend feature)
    merged["rolling_mean_7"] = (
        merged.groupby("id", observed=False)["sales"]
        .transform(lambda x: x.shift(1).rolling(window=7, min_periods=1).mean())
        .astype(np.float32)
    )

    # 2.6 Save to Feature Store
    print("\n[Step 6/6] Saving Processed Data to Feature Store...")
    data_out.mkdir(parents=True, exist_ok=True)
    
    parquet_path = data_out / "processed_m5_sales.parquet"
    try:
        merged.to_parquet(parquet_path, index=False)
        print(f"    [SUCCESS] Saved Parquet: {parquet_path}")
    except Exception as e:
        csv_path = data_out / "processed_m5_sales.csv"
        merged.to_csv(csv_path, index=False)
        print(f"    [SUCCESS] Saved CSV: {csv_path} (Reason: {e})")

    print("\n" + "=" * 65)
    print(f"  PIPELINE COMPLETE! Total Clean Records: {len(merged):,}")
    print(f"  Memory Footprint: {merged.memory_usage().sum() / 1024**2:.2f} MB")
    print("=" * 65)

    return merged


# Main Execution Entrypoint
if __name__ == "__main__":
    df_clean = run_sales_pipeline(days_window=100)

    print("\n--- SAMPLE CLEANED DATA (First 5 Rows) ---")
    preview_cols = [
        "id", "date", "sales", "sell_price", "revenue", 
        "lag_7_sales", "rolling_mean_7", "is_weekend", "snap_CA"
    ]
    print(df_clean[preview_cols].head(10).to_string())