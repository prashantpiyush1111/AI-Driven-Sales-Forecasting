package com.salesforecast.dto.response;

import java.math.BigDecimal;

public record BreakdownItem(String name, long quantity, BigDecimal revenue) {
}
