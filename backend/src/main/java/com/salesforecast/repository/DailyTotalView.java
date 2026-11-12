package com.salesforecast.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyTotalView {

    LocalDate getSaleDate();

    Long getQuantity();

    BigDecimal getRevenue();
}
