package com.salesforecast.service;

import com.salesforecast.dto.response.BreakdownItem;
import com.salesforecast.dto.response.OverviewResponse;
import com.salesforecast.dto.response.TrendPoint;
import com.salesforecast.exception.BadRequestException;
import com.salesforecast.repository.DailyTotalView;
import com.salesforecast.repository.ForecastRepository;
import com.salesforecast.repository.NameTotalView;
import com.salesforecast.repository.SalesRecordRepository;
import com.salesforecast.repository.SalesSummaryView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    public static final int MAX_TOP_LIMIT = 50;
    private static final String UNKNOWN = "Unknown";

    public enum Granularity { DAILY, MONTHLY }

    private final SalesRecordRepository salesRecordRepository;
    private final ForecastRepository forecastRepository;

    public OverviewResponse overview() {
        SalesSummaryView summary = salesRecordRepository.summarize(null, null, null, null, null);
        long productCount = salesRecordRepository.findDistinctProductNames().size();

        BigDecimal upcoming = forecastRepository.sumPredictedRevenueFrom(LocalDate.now());

        return new OverviewResponse(
                summary.getTotalQuantity() != null ? summary.getTotalQuantity() : 0L,
                summary.getTotalRevenue() != null ? summary.getTotalRevenue() : BigDecimal.ZERO,
                summary.getRecordCount() != null ? summary.getRecordCount() : 0L,
                productCount,
                forecastRepository.count(),
                upcoming != null ? upcoming : BigDecimal.ZERO);
    }

    public List<TrendPoint> trend(String productName, String category, String region,
                                  LocalDate from, LocalDate to, Granularity granularity) {
        validateRange(from, to);
        List<DailyTotalView> daily = salesRecordRepository.dailyTotals(
                clean(productName), clean(category), clean(region), from, to);

        if (granularity == Granularity.DAILY) {
            return daily.stream()
                    .map(d -> new TrendPoint(d.getSaleDate().toString(), nz(d.getQuantity()), nz(d.getRevenue())))
                    .toList();
        }

        // Monthly: bucket the daily rows in Java so the query stays database-agnostic.
        Map<YearMonth, long[]> quantities = new LinkedHashMap<>();
        Map<YearMonth, BigDecimal> revenues = new LinkedHashMap<>();
        for (DailyTotalView d : daily) {
            YearMonth month = YearMonth.from(d.getSaleDate());
            quantities.computeIfAbsent(month, k -> new long[1])[0] += nz(d.getQuantity());
            revenues.merge(month, nz(d.getRevenue()), BigDecimal::add);
        }
        return quantities.entrySet().stream()
                .map(e -> new TrendPoint(e.getKey().toString(), e.getValue()[0], revenues.get(e.getKey())))
                .toList();
    }

    public List<BreakdownItem> topProducts(int limit, LocalDate from, LocalDate to) {
        validateRange(from, to);
        int bounded = Math.min(Math.max(limit, 1), MAX_TOP_LIMIT);
        return toItems(salesRecordRepository.totalsByProduct(from, to, PageRequest.of(0, bounded)));
    }

    public List<BreakdownItem> byCategory(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return toItems(salesRecordRepository.totalsByCategory(from, to));
    }

    public List<BreakdownItem> byRegion(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return toItems(salesRecordRepository.totalsByRegion(from, to));
    }

    private List<BreakdownItem> toItems(List<NameTotalView> views) {
        return views.stream()
                .map(v -> new BreakdownItem(
                        v.getName() == null || v.getName().isBlank() ? UNKNOWN : v.getName(),
                        nz(v.getQuantity()),
                        nz(v.getRevenue())))
                .toList();
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("'from' date cannot be after 'to' date");
        }
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static long nz(Long value) {
        return value != null ? value : 0L;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
