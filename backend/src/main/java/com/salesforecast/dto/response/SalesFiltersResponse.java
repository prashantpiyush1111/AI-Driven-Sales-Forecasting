package com.salesforecast.dto.response;

import java.util.List;

public record SalesFiltersResponse(
        List<String> products,
        List<String> categories,
        List<String> regions) {
}
