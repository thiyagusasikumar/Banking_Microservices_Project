package com.example.transaction_service.client;

import com.example.transaction_service.dto.AccountOperationRequest;
import com.example.transaction_service.dto.AccountOperationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class AccountServiceClient {
    private final RestClient restClient;

    public AccountServiceClient(
            @Value("${account-service.base-url}") String baseUrl,
            @Value("${account-service.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${account-service.read-timeout-ms:5000}") int readTimeoutMs) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public AccountOperationResponse post(AccountOperationRequest operation) {
        return restClient.post()
                .uri("/api/v1/accounts/operations")
                .body(operation)
                .retrieve()
                .body(AccountOperationResponse.class);
    }

    public AccountOperationResponse get(String reference) {
        return restClient.get()
                .uri("/api/v1/accounts/operations/{reference}", reference)
                .retrieve()
                .body(AccountOperationResponse.class);
    }
}
