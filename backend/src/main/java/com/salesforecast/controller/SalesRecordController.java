package com.salesforecast.controller;

import com.salesforecast.dto.request.SalesRecordRequest;
import com.salesforecast.dto.response.SalesRecordResponse;
import com.salesforecast.dto.response.SalesSummaryResponse;
import com.salesforecast.service.SalesRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesRecordController {

    private final SalesRecordService salesRecordService;

    @GetMapping
    public ResponseEntity<Page<SalesRecordResponse>> search(
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "saleDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                salesRecordService.search(productName, category, region, from, to, pageable));
    }

    @GetMapping("/summary")
    public ResponseEntity<SalesSummaryResponse> summary(
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(
                salesRecordService.summarize(productName, category, region, from, to));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesRecordResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(salesRecordService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SalesRecordResponse> create(@Valid @RequestBody SalesRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salesRecordService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalesRecordResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody SalesRecordRequest request) {
        return ResponseEntity.ok(salesRecordService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salesRecordService.delete(id);
        return ResponseEntity.noContent().build();
    }
}