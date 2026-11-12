package com.salesforecast.dto.response;

import java.math.BigDecimal;

public record OverviewResponse(
        long totalQuantity,
        BigDecimal totalRevenue,
        long recordCount,
        long productCount,
        long forecastCount,
        BigDecimal upcomingForecastRevenue) {
}
