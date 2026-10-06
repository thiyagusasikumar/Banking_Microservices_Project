package com.banking.transactionservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.math.BigDecimal;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;

@Component
public class HttpAccountClient implements AccountClient {
    private final RestClient restClient;

    public HttpAccountClient(@Value("${account-service.base-url:http://localhost:8082}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    @CircuitBreaker(name = "accountOperations")
    @Bulkhead(name = "accountOperations")
    public Outcome apply(String reference, TransactionRecord.Type type, String sourceAccount,
                         String destinationAccount, BigDecimal amount, String currency) {
        try {
            AccountResponse response = restClient.post().uri("/internal/accounts/operations")
                    .header("Idempotency-Key", reference)
                    .body(new AccountOperation(reference, type.name(), sourceAccount,
                            destinationAccount, amount, currency))
                    .exchange((request, raw) -> {
                        HttpStatusCode status = raw.getStatusCode();
                        if (status.value() == 400 || status.value() == 404 || status.value() == 409 || status.value() == 422) {
                            return new AccountResponse("REJECTED");
                        }
                        if (!status.is2xxSuccessful()) throw new IllegalStateException("Account service returned " + status);
                        return new com.fasterxml.jackson.databind.ObjectMapper().readValue(raw.getBody(), AccountResponse.class);
                    });
            return interpret(response);
        } catch (RestClientException | java.io.UncheckedIOException ex) {
            throw new IllegalStateException("Account service response is unknown", ex);
        }
    }

    @Override
    @CircuitBreaker(name = "accountLookup")
    @Bulkhead(name = "accountLookup")
    @Retry(name = "accountLookup")
    public Outcome lookup(String reference) {
        try {
            AccountResponse response = restClient.get().uri("/internal/accounts/operations/{reference}", reference)
                    .exchange((request, raw) -> {
                        if (!raw.getStatusCode().is2xxSuccessful()) throw new IllegalStateException("Account lookup returned " + raw.getStatusCode());
                        return new com.fasterxml.jackson.databind.ObjectMapper().readValue(raw.getBody(), AccountResponse.class);
                    });
            return interpret(response);
        } catch (RestClientException | java.io.UncheckedIOException ex) {
            throw new IllegalStateException("Account lookup response is unknown", ex);
        }
    }

    private Outcome interpret(AccountResponse response) {
        if (response == null) return Outcome.UNKNOWN;
        if ("APPLIED".equals(response.status()) || "ALREADY_APPLIED".equals(response.status())) return Outcome.APPLIED;
        if ("REJECTED".equals(response.status())) return Outcome.REJECTED;
        return Outcome.UNKNOWN;
    }

    public record AccountOperation(String transactionReference, String type, String sourceAccount,
                                   String destinationAccount, BigDecimal amount, String currency) {}
    public record AccountResponse(String status) {}
}
