package com.salesforecast.service;

import com.salesforecast.dto.ml.MlPredictResponse;
import com.salesforecast.dto.response.ForecastResponse;
import com.salesforecast.entity.Forecast;
import com.salesforecast.exception.BadRequestException;
import com.salesforecast.exception.ResourceNotFoundException;
import com.salesforecast.repository.ForecastRepository;
import com.salesforecast.repository.SalesRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
class ForecastServiceTest {

    @Mock
    private ForecastRepository forecastRepository;

    @Mock
    private SalesRecordRepository salesRecordRepository;

    @Mock
    private MLClientService mlClientService;

    @InjectMocks
    private ForecastService forecastService;

    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);

    private static MlPredictResponse prediction() {
        return new MlPredictResponse(42, new BigDecimal("2100000.00"), new BigDecimal("0.8734"), "lgbm-v1");
    }

    @Test
    @DisplayName("generateForecast: calls ML, saves and returns the forecast")
    void generate_success() {
        when(salesRecordRepository.existsByProductName("Laptop Pro 15")).thenReturn(true);
        when(mlClientService.predict("Laptop Pro 15", TOMORROW)).thenReturn(prediction());
        when(forecastRepository.save(any(Forecast.class))).thenAnswer(inv -> {
            Forecast f = inv.getArgument(0);
            f.setId(7L);
            return f;
        });

        ForecastResponse response = forecastService.generateForecast("  Laptop Pro 15 ", TOMORROW);

        ArgumentCaptor<Forecast> captor = ArgumentCaptor.forClass(Forecast.class);
        verify(forecastRepository).save(captor.capture());
        assertThat(captor.getValue().getProductName()).isEqualTo("Laptop Pro 15");
        assertThat(captor.getValue().getPredictedQuantity()).isEqualTo(42);
        assertThat(captor.getValue().getModelVersion()).isEqualTo("lgbm-v1");

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getPredictedRevenue()).isEqualByComparingTo("2100000.00");
        assertThat(response.getConfidenceScore()).isEqualByComparingTo("0.8734");
    }

    @Test
    @DisplayName("generateForecast: blank product -> 400, nothing called")
    void generate_blankProduct() {
        assertThatThrownBy(() -> forecastService.generateForecast("   ", TOMORROW))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Product name is required");

        verifyNoInteractions(mlClientService, forecastRepository, salesRecordRepository);
    }

    @Test
    @DisplayName("generateForecast: null date -> 400")
    void generate_nullDate() {
        assertThatThrownBy(() -> forecastService.generateForecast("Laptop Pro 15", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Forecast date is required");

        verifyNoInteractions(mlClientService);
    }

    @Test
    @DisplayName("generateForecast: past date -> 400")
    void generate_pastDate() {
        assertThatThrownBy(() -> forecastService.generateForecast("Laptop Pro 15", LocalDate.now().minusDays(1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be in the past");

        verifyNoInteractions(mlClientService);
    }

    @Test
    @DisplayName("generateForecast: product with no sales history -> 400, ML not called")
    void generate_unknownProduct() {
        when(salesRecordRepository.existsByProductName("Ghost Product")).thenReturn(false);

        assertThatThrownBy(() -> forecastService.generateForecast("Ghost Product", TOMORROW))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No sales history");

        verifyNoInteractions(mlClientService);
        verify(forecastRepository, never()).save(any());
    }

    @Test
    @DisplayName("generateForecast: ML failure propagates and nothing is saved")
    void generate_mlFailure() {
        when(salesRecordRepository.existsByProductName("Laptop Pro 15")).thenReturn(true);
        when(mlClientService.predict("Laptop Pro 15", TOMORROW))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "down"));

        assertThatThrownBy(() -> forecastService.generateForecast("Laptop Pro 15", TOMORROW))
                .isInstanceOf(ResponseStatusException.class);

        verify(forecastRepository, never()).save(any());
    }

    @Test
    @DisplayName("getForecastsByProduct: delegates with trimmed name and date range")
    void getByProduct_success() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        Forecast f = Forecast.builder().id(1L).productName("Laptop Pro 15").forecastDate(TOMORROW)
                .predictedQuantity(10).predictedRevenue(BigDecimal.TEN).build();
        when(forecastRepository.search("Laptop Pro 15", from, to)).thenReturn(List.of(f));

        List<ForecastResponse> result = forecastService.getForecastsByProduct(" Laptop Pro 15 ", from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductName()).isEqualTo("Laptop Pro 15");
    }

    @Test
    @DisplayName("getForecastsByProduct: from after to -> 400")
    void getByProduct_invalidRange() {
        assertThatThrownBy(() -> forecastService.getForecastsByProduct(
                "Laptop Pro 15", LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(forecastRepository);
    }

    @Test
    @DisplayName("getLatestForecast: returns latest when present")
    void latest_found() {
        Forecast f = Forecast.builder().id(3L).productName("Laptop Pro 15").forecastDate(TOMORROW)
                .predictedQuantity(5).predictedRevenue(BigDecimal.ONE).build();
        when(forecastRepository.findTopByProductNameOrderByCreatedAtDesc("Laptop Pro 15"))
                .thenReturn(Optional.of(f));

        assertThat(forecastService.getLatestForecast("Laptop Pro 15").getId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getLatestForecast: none -> ResourceNotFoundException")
    void latest_notFound() {
        when(forecastRepository.findTopByProductNameOrderByCreatedAtDesc("Nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> forecastService.getLatestForecast("Nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
