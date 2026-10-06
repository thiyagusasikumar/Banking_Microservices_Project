package com.banking.transactionservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record WithdrawalRequest(@NotBlank String accountId,
                                @NotNull @DecimalMin("0.01") BigDecimal amount,
                                @NotNull @Pattern(regexp = "[A-Za-z]{3}") String currency) {}
