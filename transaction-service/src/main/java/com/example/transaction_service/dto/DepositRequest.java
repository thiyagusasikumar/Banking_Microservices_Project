package com.example.transaction_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record DepositRequest(@NotBlank String accountId,
                             @NotNull BigDecimal amount,
                             @NotNull @Pattern(regexp = "[A-Za-z]{3}") String currency) {

}
