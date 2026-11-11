package com.salesforecast.controller;

import com.salesforecast.dto.request.ForecastRequest;
import com.salesforecast.dto.response.ForecastResponse;
import com.salesforecast.service.ForecastService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/forecasts")
@RequiredArgsConstructor
public class ForecastController {

    private final ForecastService forecastService;

    @PostMapping
    public ResponseEntity<ForecastResponse> generate(@Valid @RequestBody ForecastRequest request) {
        ForecastResponse response = forecastService.generateForecast(
                request.getProductName(), request.getForecastDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{productName}")
    public ResponseEntity<List<ForecastResponse>> getByProduct(@PathVariable String productName) {
        return ResponseEntity.ok(forecastService.getForecastsByProduct(productName));
    }

    @GetMapping("/{productName}/latest")
    public ResponseEntity<ForecastResponse> getLatest(@PathVariable String productName) {
        return ResponseEntity.ok(forecastService.getLatestForecast(productName));
    }
}