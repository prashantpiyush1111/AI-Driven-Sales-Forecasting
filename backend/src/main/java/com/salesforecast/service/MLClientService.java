package com.salesforecast.service;

import com.salesforecast.dto.ml.MlPredictRequest;
import com.salesforecast.dto.ml.MlPredictResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Slf4j
@Service
public class MLClientService {

    private static final String PREDICT_PATH = "/predict";

    private final RestClient restClient;

    public MLClientService(@Qualifier("mlRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public MlPredictResponse predict(String productName, LocalDate forecastDate) {
        MlPredictResponse response;
        try {
            response = restClient.post()
                    .uri(PREDICT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new MlPredictRequest(productName, forecastDate))
                    .retrieve()
                    .body(MlPredictResponse.class);
        } catch (RestClientException ex) {
            log.warn("ML service call failed for product '{}': {}", productName, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Forecast service is currently unavailable");
        }

        if (response == null
                || response.predictedQuantity() == null
                || response.predictedRevenue() == null) {
            log.warn("ML service returned an incomplete response for product '{}'", productName);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Forecast service returned an invalid response");
        }
        return response;
    }
}