package com.banking.transactionservice.service;

import com.banking.transactionservice.entity.TransactionRecord;
import com.banking.transactionservice.dto.AccountOperationRequest;
import com.banking.transactionservice.dto.AccountOperationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.math.BigDecimal;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;

@Component
public class HttpAccountClient implements AccountClient {
    private final RestClient restClient;
    private final ObjectMapper mapper;

    public HttpAccountClient(@Qualifier("accountServiceRestClient") RestClient restClient, ObjectMapper mapper) {
        this.restClient = restClient;
        this.mapper = mapper;
    }

    @Override
    @CircuitBreaker(name = "accountOperations")
    @Bulkhead(name = "accountOperations")
    public Outcome apply(String reference, TransactionRecord.Type type, String sourceAccount,
                         String destinationAccount, BigDecimal amount, String currency) {
        try {
            AccountOperationResponse response = restClient.post().uri("/internal/accounts/operations")
                    .header("Idempotency-Key", reference)
                    .body(new AccountOperationRequest(reference, type.name(), sourceAccount,
                            destinationAccount, amount, currency))
                    .exchange((request, raw) -> {
                        HttpStatusCode status = raw.getStatusCode();
                        if (status.value() == 400 || status.value() == 404 || status.value() == 409 || status.value() == 422) {
                            return new AccountOperationResponse("REJECTED");
                        }
                        if (!status.is2xxSuccessful()) throw new IllegalStateException("Account service returned " + status);
                        return mapper.readValue(raw.getBody(), AccountOperationResponse.class);
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
            AccountOperationResponse response = restClient.get().uri("/internal/accounts/operations/{reference}", reference)
                    .exchange((request, raw) -> {
                        if (!raw.getStatusCode().is2xxSuccessful()) throw new IllegalStateException("Account lookup returned " + raw.getStatusCode());
                        return mapper.readValue(raw.getBody(), AccountOperationResponse.class);
                    });
            return interpret(response);
        } catch (RestClientException | java.io.UncheckedIOException ex) {
            throw new IllegalStateException("Account lookup response is unknown", ex);
        }
    }

    private Outcome interpret(AccountOperationResponse response) {
        if (response == null) return Outcome.UNKNOWN;
        if ("APPLIED".equals(response.status()) || "ALREADY_APPLIED".equals(response.status())) return Outcome.APPLIED;
        if ("REJECTED".equals(response.status())) return Outcome.REJECTED;
        return Outcome.UNKNOWN;
    }
}
