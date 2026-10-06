package com.banking.transactionservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService service;
    public TransactionController(TransactionService service) { this.service = service; }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionView> deposit(@RequestHeader("Idempotency-Key") String key,
                                                    @Valid @RequestBody DepositRequest request) {
        return created(service.create(TransactionRecord.Type.DEPOSIT, null, request.accountId(), request.amount(), request.currency(), key));
    }

    @PostMapping("/withdrawal")
    public ResponseEntity<TransactionView> withdrawal(@RequestHeader("Idempotency-Key") String key,
                                                       @Valid @RequestBody WithdrawalRequest request) {
        return created(service.create(TransactionRecord.Type.WITHDRAWAL, request.accountId(), null, request.amount(), request.currency(), key));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionView> transfer(@RequestHeader("Idempotency-Key") String key,
                                                     @Valid @RequestBody TransferRequest request) {
        return created(service.create(TransactionRecord.Type.TRANSFER, request.sourceAccount(), request.destinationAccount(), request.amount(), request.currency(), key));
    }

    @GetMapping("/{reference}")
    public TransactionView get(@PathVariable String reference) { return view(service.get(reference)); }

    @PostMapping("/{reference}/reconcile")
    public TransactionView reconcile(@PathVariable String reference) { return view(service.reconcile(reference)); }

    private ResponseEntity<TransactionView> created(TransactionRecord record) {
        return ResponseEntity.created(URI.create("/transactions/" + record.getReference())).body(view(record));
    }
    private TransactionView view(TransactionRecord r) {
        return new TransactionView(r.getReference(), r.getType(), r.getStatus(), r.getSourceAccount(),
                r.getDestinationAccount(), r.getAmount(), r.getCurrency(), r.getFailureReason(), r.getCreatedAt(), r.getUpdatedAt());
    }

    public record DepositRequest(@NotNull String accountId, @NotNull @DecimalMin("0.01") BigDecimal amount,
                                 @NotNull @Pattern(regexp = "[A-Za-z]{3}") String currency) {}
    public record WithdrawalRequest(@NotNull String accountId, @NotNull @DecimalMin("0.01") BigDecimal amount,
                                    @NotNull @Pattern(regexp = "[A-Za-z]{3}") String currency) {}
    public record TransferRequest(@NotNull String sourceAccount, @NotNull String destinationAccount,
                                  @NotNull @DecimalMin("0.01") BigDecimal amount,
                                  @NotNull @Pattern(regexp = "[A-Za-z]{3}") String currency) {}
    public record TransactionView(String reference, TransactionRecord.Type type, TransactionRecord.Status status,
                                  String sourceAccount, String destinationAccount, BigDecimal amount, String currency,
                                  String failureReason, Instant createdAt, Instant updatedAt) {}
}
