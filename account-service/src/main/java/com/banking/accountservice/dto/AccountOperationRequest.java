package com.banking.accountservice.dto;

import com.banking.accountservice.constant.OperationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record AccountOperationRequest(

        @NotBlank(message = "Transaction reference is required")
        String transactionReference,

        @NotNull(message = "Operation type is required")
        OperationType type,

        Long sourceAccount,

        Long destinationAccount,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Currency is required")
        @Pattern(
                regexp = "[A-Za-z]{3}",
                message = "Currency must be a valid three-letter code"
        )
        String currency

) {
}