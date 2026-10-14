package com.salesforecast.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesRecordRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name must be at most 150 characters")
    private String productName;

    @Size(max = 100, message = "Category must be at most 100 characters")
    private String category;

    @Size(max = 100, message = "Region must be at most 100 characters")
    private String region;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @NotNull(message = "Revenue is required")
    @DecimalMin(value = "0.0", message = "Revenue cannot be negative")
    @Digits(integer = 13, fraction = 2, message = "Revenue must have at most 13 digits and 2 decimal places")
    private BigDecimal revenue;

    @NotNull(message = "Sale date is required")
    private LocalDate saleDate;
}