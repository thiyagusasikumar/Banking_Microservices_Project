package com.banking.accountservice.service.impl;

import com.banking.accountservice.constant.AccountStatus;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.repository.AccountRepository;
import com.banking.accountservice.service.AccountService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public Account createAccount(CreateAccountRequest request) {

        Account account = new Account();

        account.setCustomerId(request.getCustomerId());
        account.setAccountType(request.getAccountType());
        account.setCurrency(request.getCurrency());

        account.setAccountNumber("ACC" + System.currentTimeMillis());
//        account.setAccountNumber("ACC" + UUID.randomUUID().toString().replace("-", ""));
        account.setBalance(BigDecimal.ZERO);
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        return accountRepository.save(account);
    }

    @Override
    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account not found with id: " + id
                ));
    }

    @Override
    public List<Account> getAllAccount() {
        return accountRepository.findAll();
    }

    @Override
    public BigDecimal getBalance(Long id) {

        Account account = getAccountById(id);

        return account.getBalance();
    }

    @Override
    @Transactional
    public Account updateStatus(Long id, AccountStatus status) {
        Account account = getAccountById(id);

        if (account.getAccountStatus() == AccountStatus.CLOSED && status != AccountStatus.CLOSED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Closed account cannot be reopened"
            );
        }

        if (status == AccountStatus.CLOSED && account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Account balance must be zero before closing"
            );
        }

        account.setAccountStatus(status);
        account.setUpdatedAt(LocalDateTime.now());

        return accountRepository.save(account);
    }

    @Override
    public List<Account> getAccountByCustomerId(Long customerId) {
        List<Account> accounts = accountRepository.findByCustomerId(customerId);

        if (accounts.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No accounts found for customer ID: " + customerId
            );
        }

        return accounts;
    }

    @Override
    @Transactional
    public String deleteAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account not found with id: " + id));

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Account balance must be zero before deleting"
            );
        }

        accountRepository.delete(account);

        return "Account successfully deleted";

    }
}

