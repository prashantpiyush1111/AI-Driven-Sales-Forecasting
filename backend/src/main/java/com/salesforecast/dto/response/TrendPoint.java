package com.salesforecast.dto.response;

import java.math.BigDecimal;

/** One bucket of the sales trend. {@code period} is "yyyy-MM" (monthly) or "yyyy-MM-dd" (daily). */
public record TrendPoint(String period, long quantity, BigDecimal revenue) {
}
