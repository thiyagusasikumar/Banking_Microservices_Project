package com.banking.transactionservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.banking.transactionservice.entity.TransactionRecord;
import com.banking.transactionservice.repository.TransactionRepository;
import com.banking.transactionservice.repository.OutboxRepository;
import com.banking.transactionservice.service.TransactionService;
import com.banking.transactionservice.service.AccountClient;
import com.banking.transactionservice.exception.IdempotencyConflictException;
import com.banking.transactionservice.exception.InvalidTransactionException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionServiceTests {
    @Autowired TransactionService service;
    @Autowired TransactionRepository transactions;
    @Autowired OutboxRepository outbox;
    @MockitoBean AccountClient accountClient;
    @Autowired MockMvc mvc;

    @BeforeEach
    void clear() {
        outbox.deleteAll();
        transactions.deleteAll();
        reset(accountClient);
    }

    @Test
    void completedTransferIsIdempotentAndEmitsEvents() {
        when(accountClient.apply(anyString(), eq(TransactionRecord.Type.TRANSFER), eq("A"), eq("B"),
                eq(new BigDecimal("10.00")), eq("INR"))).thenReturn(AccountClient.Outcome.APPLIED);
        TransactionRecord first = service.create(TransactionRecord.Type.TRANSFER, "A", "B",
                new BigDecimal("10.00"), "inr", "transfer-key");
        TransactionRecord repeat = service.create(TransactionRecord.Type.TRANSFER, "A", "B",
                new BigDecimal("10.00"), "INR", "transfer-key");
        assertThat(first.getStatus()).isEqualTo(TransactionRecord.Status.COMPLETED);
        assertThat(repeat.getReference()).isEqualTo(first.getReference());
        assertThat(outbox.count()).isEqualTo(2);
        verify(accountClient, times(1)).apply(anyString(), any(), any(), any(), any(), any());
    }

    @Test
    void reusedKeyForDifferentRequestIsRejected() {
        when(accountClient.apply(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(AccountClient.Outcome.REJECTED);
        service.create(TransactionRecord.Type.DEPOSIT, null, "A", new BigDecimal("5.00"), "INR", "same-key");
        assertThatThrownBy(() -> service.create(TransactionRecord.Type.DEPOSIT, null, "A",
                new BigDecimal("6.00"), "INR", "same-key"))
                .isInstanceOf(IdempotencyConflictException.class).hasMessageContaining("different request");
    }

    @Test
    void unknownResultRemainsPendingUntilLookupConfirmsIt() {
        when(accountClient.apply(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(AccountClient.Outcome.UNKNOWN);
        TransactionRecord pending = service.create(TransactionRecord.Type.WITHDRAWAL, "A", null,
                new BigDecimal("5.00"), "INR", "unknown-key");
        assertThat(pending.getStatus()).isEqualTo(TransactionRecord.Status.PENDING_RECONCILIATION);
        when(accountClient.lookup(pending.getReference())).thenReturn(AccountClient.Outcome.UNKNOWN);
        assertThat(service.reconcile(pending.getReference()).getStatus())
                .isEqualTo(TransactionRecord.Status.PENDING_RECONCILIATION);
        when(accountClient.lookup(pending.getReference())).thenReturn(AccountClient.Outcome.APPLIED);
        assertThat(service.reconcile(pending.getReference()).getStatus())
                .isEqualTo(TransactionRecord.Status.COMPLETED);
        assertThat(outbox.count()).isEqualTo(3);
        verify(accountClient, times(1)).apply(anyString(), any(), any(), any(), any(), any());
    }

    @Test
    void sameSourceAndDestinationIsRejectedBeforeCallingAccounts() {
        assertThatThrownBy(() -> service.create(TransactionRecord.Type.TRANSFER, "A", "A",
                new BigDecimal("1.00"), "INR", "bad-key"))
                .isInstanceOf(InvalidTransactionException.class);
        verifyNoInteractions(accountClient);
    }

    @Test
    void transferHttpApiCreatesAndReadsTransaction() throws Exception {
        when(accountClient.apply(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(AccountClient.Outcome.APPLIED);
        String response = mvc.perform(post("/transactions/transfer")
                        .header("Idempotency-Key", "http-key")
                        .contentType("application/json")
                        .content("{\"sourceAccount\":\"A\",\"destinationAccount\":\"B\",\"amount\":12.50,\"currency\":\"INR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn().getResponse().getContentAsString();
        String reference = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("reference").asText();
        mvc.perform(get("/transactions/{reference}", reference))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(12.50));
    }

    @Test
    void httpErrorsUseExpectedStatusCodes() throws Exception {
        when(accountClient.apply(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(AccountClient.Outcome.REJECTED);
        mvc.perform(post("/transactions/deposit")
                        .header("Idempotency-Key", "duplicate-key")
                        .contentType("application/json")
                        .content("{\"accountId\":\"A\",\"amount\":5.00,\"currency\":\"INR\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/transactions/deposit")
                        .header("Idempotency-Key", "duplicate-key")
                        .contentType("application/json")
                        .content("{\"accountId\":\"A\",\"amount\":6.00,\"currency\":\"INR\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Idempotency key belongs to a different request"));
        mvc.perform(get("/transactions/missing-reference"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/transactions/deposit")
                        .header("Idempotency-Key", "invalid-body")
                        .contentType("application/json")
                        .content("{\"accountId\":\"\",\"amount\":5.00,\"currency\":\"INR\"}"))
                .andExpect(status().isBadRequest());
    }
}
