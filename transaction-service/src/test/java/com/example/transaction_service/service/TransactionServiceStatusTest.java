package com.example.transaction_service.service;

import com.example.transaction_service.client.AccountServiceClient;
import com.example.transaction_service.constants.Status;
import com.example.transaction_service.constants.Type;
import com.example.transaction_service.dto.AccountOperationResponse;
import com.example.transaction_service.dto.AccountOperationRequest;
import com.example.transaction_service.entity.TransactionRecord;
import com.example.transaction_service.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceStatusTest {

    @Mock
    private TransactionRepository transactions;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private AccountServiceClient accountServiceClient;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactions, transactionTemplate, accountServiceClient);
    }

    @Test
    void appliedPostingIsPersistedAsCompleted() {
        TransactionRecord record = pendingRecord();
        when(transactions.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(transactions.findByReferenceForUpdate(any())).thenAnswer(invocation -> {
            record.setReference(invocation.getArgument(0));
            return Optional.of(record);
        });
        when(accountServiceClient.post(any())).thenAnswer(invocation -> {
            AccountOperationRequest request = invocation.getArgument(0);
            return new AccountOperationResponse(request.transactionReference(),
                    com.example.transaction_service.constants.PostingStatus.APPLIED, null);
        });
        executeCallbacks();

        TransactionRecord result = transactionService.create(
                Type.DEPOSIT, null, "14", new BigDecimal("500.00"), "INR", "key-1");

        assertEquals(Status.COMPLETED, result.getStatus());
    }

    private void executeCallbacks() {
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    TransactionCallback<TransactionRecord> callback =
                            (TransactionCallback<TransactionRecord>) invocation.getArgument(0);
                    return callback.doInTransaction(mock(org.springframework.transaction.TransactionStatus.class));
                });
    }

    private TransactionRecord pendingRecord() {
        TransactionRecord record = new TransactionRecord();
        record.setId(UUID.randomUUID());
        record.setReference(UUID.randomUUID().toString());
        record.setType(Type.DEPOSIT);
        record.setDestinationAccount("14");
        record.setAmount(new BigDecimal("500.00"));
        record.setCurrency("INR");
        record.setStatus(Status.INITIATED);
        record.setIdempotencyKey("key-1");
        record.setCreatedAt(Instant.now());
        record.setUpdatedAt(Instant.now());
        return record;
    }
}
