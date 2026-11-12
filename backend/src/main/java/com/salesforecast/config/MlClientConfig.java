package com.salesforecast.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class MlClientConfig {

    @Bean
    @Qualifier("mlRestClient")
    public RestClient mlRestClient(RestClient.Builder builder,
                                   @Value("${ml.service.base-url:http://localhost:8000}") String baseUrl,
                                   @Value("${ml.service.timeout-ms:5000}") int timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        return builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
