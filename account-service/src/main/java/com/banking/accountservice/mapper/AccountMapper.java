package com.banking.accountservice.mapper;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.entity.Account;

public final class AccountMapper {

    private AccountMapper() {
    }

    public static AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency(),
                account.getAccountStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }
}
