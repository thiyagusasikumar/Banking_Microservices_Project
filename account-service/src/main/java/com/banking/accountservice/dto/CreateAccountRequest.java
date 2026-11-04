package com.banking.accountservice.dto;

import com.banking.accountservice.constant.AccountType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAccountRequest {

    // Set by AccountController from the validated access token, never from request JSON.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long customerId;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "INR", message = "Only INR currency is supported")
    private String currency;
}