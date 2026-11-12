package com.salesforecast.service;

import com.salesforecast.dto.ml.MlPredictResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.http.HttpMethod;

class MLClientServiceTest {

    private MockRestServiceServer server;
    private MLClientService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ml-service");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new MLClientService(builder.build());
    }

    @Test
    @DisplayName("predict: maps snake_case ML response")
    void predict_success() {
        server.expect(requestTo("http://ml-service/predict"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.product_name").value("Laptop Pro 15"))
                .andRespond(withSuccess("""
                        {"predicted_quantity": 42, "predicted_revenue": 2100000.50,
                         "confidence_score": 0.87, "model_version": "lgbm-v1", "extra": "ignored"}
                        """, MediaType.APPLICATION_JSON));

        MlPredictResponse response = service.predict("Laptop Pro 15", LocalDate.of(2026, 12, 1));

        assertThat(response.predictedQuantity()).isEqualTo(42);
        assertThat(response.predictedRevenue()).isEqualByComparingTo("2100000.50");
        assertThat(response.modelVersion()).isEqualTo("lgbm-v1");
        server.verify();
    }

    @Test
    @DisplayName("predict: ML 5xx -> 503")
    void predict_serverError() {
        server.expect(requestTo("http://ml-service/predict")).andRespond(withServerError());

        assertThatThrownBy(() -> service.predict("Laptop Pro 15", LocalDate.of(2026, 12, 1)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("predict: incomplete ML response -> 502")
    void predict_incompleteResponse() {
        server.expect(requestTo("http://ml-service/predict"))
                .andRespond(withSuccess("{\"model_version\": \"x\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.predict("Laptop Pro 15", LocalDate.of(2026, 12, 1)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
