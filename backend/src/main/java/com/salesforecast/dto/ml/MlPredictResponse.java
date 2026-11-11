package com.salesforecast.dto.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MlPredictResponse(
        @JsonProperty("predicted_quantity") Integer predictedQuantity,
        @JsonProperty("predicted_revenue") BigDecimal predictedRevenue,
        @JsonProperty("confidence_score") BigDecimal confidenceScore,
        @JsonProperty("model_version") String modelVersion) {
}