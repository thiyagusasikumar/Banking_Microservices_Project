package com.banking.accountservice.dto;

import com.banking.accountservice.constant.AccountType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAccountRequest {


    private AccountType accountType;

    // Currency may be omitted for an account-type update; supplied values must be INR.
    private String currency;
}
