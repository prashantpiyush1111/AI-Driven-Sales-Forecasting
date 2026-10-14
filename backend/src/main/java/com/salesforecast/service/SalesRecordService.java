package com.salesforecast.service;

import com.salesforecast.dto.request.SalesRecordRequest;
import com.salesforecast.dto.response.SalesRecordResponse;
import com.salesforecast.dto.response.SalesSummaryResponse;
import com.salesforecast.entity.SalesRecord;
import com.salesforecast.exception.BadRequestException;
import com.salesforecast.exception.ResourceNotFoundException;
import com.salesforecast.repository.SalesRecordRepository;
import com.salesforecast.repository.SalesSummaryView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesRecordService {

    private static final String RESOURCE_NAME = "Sales record";

    private final SalesRecordRepository salesRecordRepository;

    public Page<SalesRecordResponse> search(String productName, String category, String region,
                                            LocalDate from, LocalDate to, Pageable pageable) {
        validateDateRange(from, to);
        return salesRecordRepository
                .search(clean(productName), clean(category), clean(region), from, to, pageable)
                .map(this::toResponse);
    }

    public SalesSummaryResponse summarize(String productName, String category, String region,
                                          LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        SalesSummaryView view = salesRecordRepository
                .summarize(clean(productName), clean(category), clean(region), from, to);

        long totalQuantity = view.getTotalQuantity() != null ? view.getTotalQuantity() : 0L;
        BigDecimal totalRevenue = view.getTotalRevenue() != null ? view.getTotalRevenue() : BigDecimal.ZERO;
        long recordCount = view.getRecordCount() != null ? view.getRecordCount() : 0L;
        return new SalesSummaryResponse(totalQuantity, totalRevenue, recordCount);
    }

    public SalesRecordResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public SalesRecordResponse create(SalesRecordRequest request) {
        SalesRecord record = new SalesRecord();
        applyRequest(record, request);
        return toResponse(salesRecordRepository.save(record));
    }

    @Transactional
    public SalesRecordResponse update(Long id, SalesRecordRequest request) {
        SalesRecord record = findOrThrow(id);
        applyRequest(record, request);
        return toResponse(salesRecordRepository.save(record));
    }

    @Transactional
    public void delete(Long id) {
        if (!salesRecordRepository.existsById(id)) {
            throw new ResourceNotFoundException(RESOURCE_NAME, id);
        }
        salesRecordRepository.deleteById(id);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("'from' date cannot be after 'to' date");
        }
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private SalesRecord findOrThrow(Long id) {
        return salesRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NAME, id));
    }

    private void applyRequest(SalesRecord record, SalesRecordRequest request) {
        record.setProductName(request.getProductName().trim());
        record.setCategory(request.getCategory());
        record.setRegion(request.getRegion());
        record.setQuantity(request.getQuantity());
        record.setRevenue(request.getRevenue());
        record.setSaleDate(request.getSaleDate());
    }

    private SalesRecordResponse toResponse(SalesRecord record) {
        return SalesRecordResponse.builder()
                .id(record.getId())
                .productName(record.getProductName())
                .category(record.getCategory())
                .region(record.getRegion())
                .quantity(record.getQuantity())
                .revenue(record.getRevenue())
                .saleDate(record.getSaleDate())
                .createdAt(record.getCreatedAt())
                .build();
    }
}