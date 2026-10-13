package com.banking.accountservice.service;


import com.banking.accountservice.constant.AccountStatus;
import com.banking.accountservice.constant.AccountType;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.dto.UpdateAccountRequest;
import com.banking.accountservice.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    Account createAccount(CreateAccountRequest request);

    Account getAccountById(Long id);

    Account getByAccountNumber(String accountNumber);

    List<Account> getMyAccounts(Long customerId);

    List<Account> getByCustomerId(Long customerId);

    Page<Account> getAllAccounts(AccountStatus status, AccountType type, Pageable pageable);

    Account updateAccount(Long id, UpdateAccountRequest request);

    BigDecimal getBalance(Long id);
}
