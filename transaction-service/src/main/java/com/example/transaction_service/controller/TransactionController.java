package com.example.transaction_service.controller;

import com.example.transaction_service.constants.Type;
import com.example.transaction_service.dto.DepositRequest;
import com.example.transaction_service.dto.TransactionResponse;
import com.example.transaction_service.dto.WithdrawalRequest;
import com.example.transaction_service.entity.TransactionRecord;
import com.example.transaction_service.mapper.TransactionMapper;
import com.example.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService service;
    private final TransactionMapper mapper;


    public TransactionController(TransactionService service, TransactionMapper mapper) {
        this.service = service;
        this.mapper = mapper;

    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@RequestHeader("Idempotency-Key") String key,
                                                       @Valid @RequestBody DepositRequest request) {

        return created(service.create(Type.DEPOSIT, null, request.accountId(), request.amount(), request.currency(), key));

    }

    @PostMapping("/withdrawal")
    public ResponseEntity<TransactionResponse> withdrawal(@RequestHeader("Idempotency-Key") String key,
                                                          @Valid @RequestBody WithdrawalRequest request) {
        return created(service.create(Type.WITHDRAWAL, request.accountId(), null, request.amount(), request.currency(), key));
    }

    private ResponseEntity<TransactionResponse> created(TransactionRecord record) {

        return ResponseEntity
                .created(URI.create(record.getReference()))
                .body(mapper.toResponse(record));
    }

}
