package com.salesforecast.controller;

import com.salesforecast.dto.response.BreakdownItem;
import com.salesforecast.dto.response.OverviewResponse;
import com.salesforecast.dto.response.TrendPoint;
import com.salesforecast.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<OverviewResponse> overview() {
        return ResponseEntity.ok(analyticsService.overview());
    }

    @GetMapping("/trend")
    public ResponseEntity<List<TrendPoint>> trend(
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "MONTHLY") AnalyticsService.Granularity granularity) {
        return ResponseEntity.ok(analyticsService.trend(productName, category, region, from, to, granularity));
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<BreakdownItem>> topProducts(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.topProducts(limit, from, to));
    }

    @GetMapping("/by-category")
    public ResponseEntity<List<BreakdownItem>> byCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.byCategory(from, to));
    }

    @GetMapping("/by-region")
    public ResponseEntity<List<BreakdownItem>> byRegion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.byRegion(from, to));
    }
}
