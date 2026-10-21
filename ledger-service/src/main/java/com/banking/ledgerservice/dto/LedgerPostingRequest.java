package com.banking.ledgerservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record LedgerPostingRequest(
        @NotNull(message = "Transaction ID is required") UUID transactionId,
        @NotNull(message = "Debit account ID is required") @Positive(message = "Debit account ID must be positive") Long debitAccountId,
        @NotNull(message = "Credit account ID is required") @Positive(message = "Credit account ID must be positive") Long creditAccountId,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "Amount must have at most 17 whole digits and 2 decimal places")
        BigDecimal amount,
        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "[A-Za-z]{3}", message = "Currency must be a three-letter code")
        String currency
) {
}
