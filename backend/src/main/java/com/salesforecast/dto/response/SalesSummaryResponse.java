package com.salesforecast.dto.response;

import java.math.BigDecimal;

public record SalesSummaryResponse(
        long totalQuantity,
        BigDecimal totalRevenue,
        long recordCount) {
}