package com.salesforecast.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record MlPredictRequest(
        @JsonProperty("product_name") String productName,
        @JsonProperty("forecast_date") LocalDate forecastDate) {
}