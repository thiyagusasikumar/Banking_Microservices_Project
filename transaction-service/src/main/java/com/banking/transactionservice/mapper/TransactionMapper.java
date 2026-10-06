package com.banking.transactionservice.mapper;

import com.banking.transactionservice.dto.TransactionResponse;
import com.banking.transactionservice.entity.TransactionRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Component
public class TransactionMapper {
    private final ObjectMapper objectMapper;

    public TransactionMapper(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public TransactionResponse toResponse(TransactionRecord record) {
        return new TransactionResponse(record.getReference(), record.getType(), record.getStatus(),
                record.getSourceAccount(), record.getDestinationAccount(), record.getAmount(),
                record.getCurrency(), record.getFailureReason(), record.getCreatedAt(), record.getUpdatedAt());
    }

    public String toEventPayload(TransactionRecord record) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "transactionReference", record.getReference(),
                    "type", record.getType().name(),
                    "status", record.getStatus().name(),
                    "amount", record.getAmount(),
                    "currency", record.getCurrency(),
                    "sourceAccount", record.getSourceAccount() == null ? "" : record.getSourceAccount(),
                    "destinationAccount", record.getDestinationAccount() == null ? "" : record.getDestinationAccount()));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize transaction event", ex);
        }
    }
}
