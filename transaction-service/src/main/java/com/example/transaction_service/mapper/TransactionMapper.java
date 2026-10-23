package com.example.transaction_service.mapper;

import com.example.transaction_service.dto.TransactionResponse;
import com.example.transaction_service.entity.TransactionRecord;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(TransactionRecord record) {
        return new TransactionResponse(record.getReference(), record.getType(), record.getStatus(),
                record.getSourceAccount(), record.getDestinationAccount(), record.getAmount(),
                record.getCurrency(), record.getFailureReason(), record.getCreatedAt(), record.getUpdatedAt());
    }

}

