package com.banking.accountservice.service;


import com.banking.accountservice.constant.AccountStatus;
import com.banking.accountservice.constant.AccountType;
import com.banking.accountservice.dto.*;
import com.banking.accountservice.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccountById(Long id);

    AccountResponse getByAccountNumber(String accountNumber);

    List<AccountResponse> getMyAccounts(Long customerId);

    List<AccountResponse> getByCustomerId(Long customerId);

    Page<AccountResponse> getAllAccounts(AccountStatus status, AccountType type, Pageable pageable);

    AccountResponse updateAccount(Long id, UpdateAccountRequest request);

    BalanceResponse getBalance(Long id);
}
