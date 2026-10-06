package com.banking.transactionservice.controller;

import com.banking.transactionservice.entity.TransactionRecord;
import com.banking.transactionservice.service.TransactionService;
import com.banking.transactionservice.mapper.TransactionMapper;
import com.banking.transactionservice.dto.DepositRequest;
import com.banking.transactionservice.dto.WithdrawalRequest;
import com.banking.transactionservice.dto.TransferRequest;
import com.banking.transactionservice.dto.TransactionResponse;
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
        return created(service.create(TransactionRecord.Type.DEPOSIT, null, request.accountId(), request.amount(), request.currency(), key));
    }

    @PostMapping("/withdrawal")
    public ResponseEntity<TransactionResponse> withdrawal(@RequestHeader("Idempotency-Key") String key,
                                                       @Valid @RequestBody WithdrawalRequest request) {
        return created(service.create(TransactionRecord.Type.WITHDRAWAL, request.accountId(), null, request.amount(), request.currency(), key));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@RequestHeader("Idempotency-Key") String key,
                                                     @Valid @RequestBody TransferRequest request) {
        return created(service.create(TransactionRecord.Type.TRANSFER, request.sourceAccount(), request.destinationAccount(), request.amount(), request.currency(), key));
    }

    @GetMapping("/{reference}")
    public TransactionResponse get(@PathVariable String reference) { return mapper.toResponse(service.get(reference)); }

    @PostMapping("/{reference}/reconcile")
    public TransactionResponse reconcile(@PathVariable String reference) { return mapper.toResponse(service.reconcile(reference)); }

    private ResponseEntity<TransactionResponse> created(TransactionRecord record) {
        return ResponseEntity.created(URI.create("/transactions/" + record.getReference()))
                .body(mapper.toResponse(record));
    }
}
