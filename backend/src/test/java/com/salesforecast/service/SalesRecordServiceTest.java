package com.salesforecast.service;

import com.salesforecast.dto.request.SalesRecordRequest;
import com.salesforecast.dto.response.SalesRecordResponse;
import com.salesforecast.dto.response.SalesSummaryResponse;
import com.salesforecast.entity.SalesRecord;
import com.salesforecast.exception.BadRequestException;
import com.salesforecast.exception.ResourceNotFoundException;
import com.salesforecast.repository.SalesRecordRepository;
import com.salesforecast.repository.SalesSummaryView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesRecordServiceTest {

    @Mock
    private SalesRecordRepository salesRecordRepository;

    @InjectMocks
    private SalesRecordService salesRecordService;

    private static SalesRecord sampleRecord() {
        return SalesRecord.builder()
                .id(1L)
                .productName("Laptop Pro 15")
                .category("Electronics")
                .region("North")
                .quantity(5)
                .revenue(new BigDecimal("4999.50"))
                .saleDate(LocalDate.of(2026, 1, 15))
                .build();
    }

    private static SalesRecordRequest sampleRequest() {
        return SalesRecordRequest.builder()
                .productName("  Laptop Pro 15  ")
                .category("Electronics")
                .region("North")
                .quantity(5)
                .revenue(new BigDecimal("4999.50"))
                .saleDate(LocalDate.of(2026, 1, 15))
                .build();
    }

    @Test
    @DisplayName("search: trims filters, converts blank to null, maps to response")
    void search_cleansFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<SalesRecord> page = new PageImpl<>(List.of(sampleRecord()), pageable, 1);
        when(salesRecordRepository.search("Laptop", null, null, null, null, pageable)).thenReturn(page);

        Page<SalesRecordResponse> result =
                salesRecordService.search("  Laptop ", "   ", null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getProductName()).isEqualTo("Laptop Pro 15");
    }

    @Test
    @DisplayName("search: from after to -> BadRequestException, repository never called")
    void search_invalidDateRange_throws() {
        assertThatThrownBy(() -> salesRecordService.search(
                null, null, null,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1),
                PageRequest.of(0, 20)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("'from' date cannot be after 'to' date");

        verifyNoInteractions(salesRecordRepository);
    }

    @Test
    @DisplayName("summarize: null aggregates (no matching rows) become zeros")
    void summarize_nullAggregates_becomeZero() {
        SalesSummaryView view = org.mockito.Mockito.mock(SalesSummaryView.class);
        when(view.getTotalQuantity()).thenReturn(null);
        when(view.getTotalRevenue()).thenReturn(null);
        when(view.getRecordCount()).thenReturn(null);
        when(salesRecordRepository.summarize(null, null, null, null, null)).thenReturn(view);

        SalesSummaryResponse result = salesRecordService.summarize(null, null, null, null, null);

        assertThat(result.totalQuantity()).isZero();
        assertThat(result.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.recordCount()).isZero();
    }

    @Test
    @DisplayName("summarize: returns aggregates from repository")
    void summarize_returnsValues() {
        SalesSummaryView view = org.mockito.Mockito.mock(SalesSummaryView.class);
        when(view.getTotalQuantity()).thenReturn(120L);
        when(view.getTotalRevenue()).thenReturn(new BigDecimal("15000.75"));
        when(view.getRecordCount()).thenReturn(8L);
        when(salesRecordRepository.summarize("Laptop", null, "North", null, null)).thenReturn(view);

        SalesSummaryResponse result =
                salesRecordService.summarize(" Laptop ", "", "North", null, null);

        assertThat(result.totalQuantity()).isEqualTo(120L);
        assertThat(result.totalRevenue()).isEqualByComparingTo("15000.75");
        assertThat(result.recordCount()).isEqualTo(8L);
    }

    @Test
    @DisplayName("getById: unknown id -> ResourceNotFoundException")
    void getById_notFound_throws() {
        when(salesRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salesRecordService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Sales record not found with id: 99");
    }

    @Test
    @DisplayName("create: trims product name and saves all fields")
    void create_savesTrimmedRecord() {
        when(salesRecordRepository.save(any(SalesRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        SalesRecordResponse response = salesRecordService.create(sampleRequest());

        ArgumentCaptor<SalesRecord> captor = ArgumentCaptor.forClass(SalesRecord.class);
        verify(salesRecordRepository).save(captor.capture());
        SalesRecord saved = captor.getValue();

        assertThat(saved.getProductName()).isEqualTo("Laptop Pro 15");
        assertThat(saved.getQuantity()).isEqualTo(5);
        assertThat(saved.getRevenue()).isEqualByComparingTo("4999.50");
        assertThat(saved.getSaleDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(response.getProductName()).isEqualTo("Laptop Pro 15");
    }

    @Test
    @DisplayName("update: unknown id -> ResourceNotFoundException, nothing saved")
    void update_notFound_throws() {
        when(salesRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salesRecordService.update(99L, sampleRequest()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(salesRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete: unknown id -> ResourceNotFoundException, deleteById never called")
    void delete_notFound_throws() {
        when(salesRecordRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> salesRecordService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Sales record not found with id: 99");

        verify(salesRecordRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete: existing id -> deleteById called")
    void delete_existing_deletes() {
        when(salesRecordRepository.existsById(1L)).thenReturn(true);

        salesRecordService.delete(1L);

        verify(salesRecordRepository).deleteById(1L);
    }
}