package com.salesforecast.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesRecordResponse {

    private Long id;

    private String productName;

    private String category;

    private String region;

    private Integer quantity;

    private BigDecimal revenue;

    private LocalDate saleDate;

    private LocalDateTime createdAt;
}