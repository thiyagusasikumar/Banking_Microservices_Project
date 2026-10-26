package com.banking.accountservice.dto;

import com.banking.accountservice.constant.AccountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAccountRequest {


    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @Pattern(regexp = "INR", message = "Only INR currency is supported")
    private String currency;
}
