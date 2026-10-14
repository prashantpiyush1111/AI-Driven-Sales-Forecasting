package com.salesforecast.repository;

import java.math.BigDecimal;

public interface SalesSummaryView {

    Long getTotalQuantity();

    BigDecimal getTotalRevenue();

    Long getRecordCount();
}