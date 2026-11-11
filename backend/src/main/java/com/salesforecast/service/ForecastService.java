package com.salesforecast.service;

import com.salesforecast.dto.ml.MlPredictResponse;
import com.salesforecast.dto.response.ForecastResponse;
import com.salesforecast.entity.Forecast;
import com.salesforecast.exception.BadRequestException;
import com.salesforecast.exception.ResourceNotFoundException;
import com.salesforecast.repository.ForecastRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ForecastService {

    private final ForecastRepository forecastRepository;
    private final MLClientService mlClientService;

    // Intentionally not @Transactional: the slow HTTP call to the ML service
    // must not hold a database connection open. save() runs in its own transaction.
    public ForecastResponse generateForecast(String productName, LocalDate forecastDate) {
        if (productName == null || productName.isBlank()) {
            throw new BadRequestException("Product name is required");
        }
        if (forecastDate == null) {
            throw new BadRequestException("Forecast date is required");
        }
        if (forecastDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Forecast date cannot be in the past");
        }

        String product = productName.trim();
        MlPredictResponse prediction = mlClientService.predict(product, forecastDate);

        Forecast forecast = Forecast.builder()
                .productName(product)
                .forecastDate(forecastDate)
                .predictedQuantity(prediction.predictedQuantity())
                .predictedRevenue(prediction.predictedRevenue())
                .confidenceScore(prediction.confidenceScore())
                .modelVersion(prediction.modelVersion())
                .build();

        return toResponse(forecastRepository.save(forecast));
    }

    @Transactional(readOnly = true)
    public List<ForecastResponse> getForecastsByProduct(String productName) {
        return forecastRepository.findByProductNameOrderByForecastDateAsc(productName.trim())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ForecastResponse getLatestForecast(String productName) {
        return forecastRepository.findTopByProductNameOrderByCreatedAtDesc(productName.trim())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No forecast found for product: " + productName));
    }

    private ForecastResponse toResponse(Forecast forecast) {
        return ForecastResponse.builder()
                .id(forecast.getId())
                .productName(forecast.getProductName())
                .forecastDate(forecast.getForecastDate())
                .predictedQuantity(forecast.getPredictedQuantity())
                .predictedRevenue(forecast.getPredictedRevenue())
                .confidenceScore(forecast.getConfidenceScore())
                .modelVersion(forecast.getModelVersion())
                .createdAt(forecast.getCreatedAt())
                .build();
    }
}