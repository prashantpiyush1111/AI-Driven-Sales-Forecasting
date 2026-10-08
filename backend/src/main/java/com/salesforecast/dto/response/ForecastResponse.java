package com.salesforecast.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForecastResponse {

    private Long id;
    private String productName;
    private LocalDate forecastDate;
    private Integer predictedQuantity;
    private BigDecimal predictedRevenue;
    private BigDecimal confidenceScore;
    private String modelVersion;
    private LocalDateTime createdAt;
}