package com.banking.accountservice.service;


import com.banking.accountservice.constant.AccountStatus;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.entity.Account;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    Account createAccount(CreateAccountRequest request);

    Account getAccountById(Long id);

    List<Account> getAllAccount();

    BigDecimal getBalance(Long id);

    Account updateStatus(Long id, AccountStatus status);

    List<Account> getAccountByCustomerId(Long customerId);

    String deleteAccount(Long id);
}
