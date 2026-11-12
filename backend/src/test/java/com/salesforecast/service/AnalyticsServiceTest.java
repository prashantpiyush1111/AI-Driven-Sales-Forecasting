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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private SalesRecordRepository salesRecordRepository;

    @Mock
    private ForecastRepository forecastRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private static DailyTotalView daily(LocalDate date, long qty, String revenue) {
        return new DailyTotalView() {
            public LocalDate getSaleDate() { return date; }
            public Long getQuantity() { return qty; }
            public BigDecimal getRevenue() { return new BigDecimal(revenue); }
        };
    }

    private static NameTotalView named(String name, long qty, String revenue) {
        return new NameTotalView() {
            public String getName() { return name; }
            public Long getQuantity() { return qty; }
            public BigDecimal getRevenue() { return revenue == null ? null : new BigDecimal(revenue); }
        };
    }

    @Test
    @DisplayName("trend MONTHLY: buckets daily rows by month, in order")
    void trend_monthly() {
        when(salesRecordRepository.dailyTotals(any(), any(), any(), any(), any())).thenReturn(List.of(
                daily(LocalDate.of(2026, 1, 5), 10, "100.00"),
                daily(LocalDate.of(2026, 1, 20), 5, "50.50"),
                daily(LocalDate.of(2026, 2, 3), 7, "70.00")));

        List<TrendPoint> trend = analyticsService.trend(null, null, null, null, null,
                AnalyticsService.Granularity.MONTHLY);

        assertThat(trend).hasSize(2);
        assertThat(trend.get(0).period()).isEqualTo("2026-01");
        assertThat(trend.get(0).quantity()).isEqualTo(15);
        assertThat(trend.get(0).revenue()).isEqualByComparingTo("150.50");
        assertThat(trend.get(1).period()).isEqualTo("2026-02");
    }

    @Test
    @DisplayName("trend DAILY: one point per day")
    void trend_daily() {
        when(salesRecordRepository.dailyTotals(any(), any(), any(), any(), any())).thenReturn(List.of(
                daily(LocalDate.of(2026, 1, 5), 10, "100.00")));

        List<TrendPoint> trend = analyticsService.trend(null, null, null, null, null,
                AnalyticsService.Granularity.DAILY);

        assertThat(trend).singleElement().satisfies(p -> assertThat(p.period()).isEqualTo("2026-01-05"));
    }

    @Test
    @DisplayName("trend: blank filters are normalised to null; invalid range -> 400")
    void trend_filtersAndValidation() {
        when(salesRecordRepository.dailyTotals(eq(null), eq("Electronics"), eq(null), any(), any()))
                .thenReturn(List.of());

        assertThat(analyticsService.trend("  ", " Electronics ", "", null, null,
                AnalyticsService.Granularity.MONTHLY)).isEmpty();

        assertThatThrownBy(() -> analyticsService.trend(null, null, null,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 1, 1), AnalyticsService.Granularity.MONTHLY))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("topProducts: limit is clamped to [1, MAX_TOP_LIMIT]")
    void topProducts_clampsLimit() {
        when(salesRecordRepository.totalsByProduct(any(), any(), any(Pageable.class)))
                .thenReturn(List.of(named("Laptop Pro 15", 43, "2150000.00")));

        List<BreakdownItem> items = analyticsService.topProducts(10_000, null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(salesRecordRepository).totalsByProduct(any(), any(), captor.capture());
        assertThat(captor.getValue()).isEqualTo(PageRequest.of(0, AnalyticsService.MAX_TOP_LIMIT));
        assertThat(items).hasSize(1);
    }

    @Test
    @DisplayName("byCategory: null names become 'Unknown'")
    void byCategory_unknownName() {
        when(salesRecordRepository.totalsByCategory(any(), any()))
                .thenReturn(List.of(named(null, 3, "30.00")));

        assertThat(analyticsService.byCategory(null, null))
                .singleElement()
                .satisfies(i -> assertThat(i.name()).isEqualTo("Unknown"));
    }

    @Test
    @DisplayName("overview: combines sales summary and forecast stats, null-safe")
    void overview() {
        SalesSummaryView summary = new SalesSummaryView() {
            public Long getTotalQuantity() { return 308L; }
            public BigDecimal getTotalRevenue() { return new BigDecimal("2680000.00"); }
            public Long getRecordCount() { return 6L; }
        };
        when(salesRecordRepository.summarize(null, null, null, null, null)).thenReturn(summary);
        when(salesRecordRepository.findDistinctProductNames()).thenReturn(List.of("A", "B", "C"));
        when(forecastRepository.count()).thenReturn(4L);
        when(forecastRepository.sumPredictedRevenueFrom(any(LocalDate.class))).thenReturn(null);

        OverviewResponse overview = analyticsService.overview();

        assertThat(overview.totalQuantity()).isEqualTo(308L);
        assertThat(overview.productCount()).isEqualTo(3L);
        assertThat(overview.forecastCount()).isEqualTo(4L);
        assertThat(overview.upcomingForecastRevenue()).isEqualByComparingTo("0");
    }
}
