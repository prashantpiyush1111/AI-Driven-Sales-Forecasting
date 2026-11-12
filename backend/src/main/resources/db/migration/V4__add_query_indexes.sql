-- Indexes for the filter, analytics and forecast-history queries.
CREATE INDEX idx_sales_product_date ON sales_records(product_name, sale_date);
CREATE INDEX idx_sales_category ON sales_records(category);
CREATE INDEX idx_sales_region ON sales_records(region);
CREATE INDEX idx_forecast_product_date ON forecasts(product_name, forecast_date);
