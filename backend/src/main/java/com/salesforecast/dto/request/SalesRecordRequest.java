package com.salesforecast.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesRecordRequest {

    @NotBlank(message = "Product name is required")
    private String productName;

    private String category;

    private String region;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @NotNull(message = "Revenue is required")
    @DecimalMin(value = "0.0", message = "Revenue cannot be negative")
    private BigDecimal revenue;

    @NotNull(message = "Sale date is required")
    private LocalDate saleDate;
}