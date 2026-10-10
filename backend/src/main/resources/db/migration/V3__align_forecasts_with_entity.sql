ALTER TABLE forecasts
    ADD COLUMN predicted_quantity INT NOT NULL DEFAULT 0 AFTER forecast_date;

ALTER TABLE forecasts
    CHANGE COLUMN model_used model_version VARCHAR(50);