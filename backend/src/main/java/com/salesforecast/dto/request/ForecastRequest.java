package com.salesforecast.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForecastRequest {

    @NotBlank(message = "Product name is required")
    private String productName;

    @NotNull(message = "Forecast date is required")
    @FutureOrPresent(message = "Forecast date cannot be in the past")
    private LocalDate forecastDate;
}