package com.banking.accountservice.service.impl;

import com.banking.accountservice.constant.AccountStatus;
import com.banking.accountservice.constant.AccountType;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.dto.UpdateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.repository.AccountRepository;
import com.banking.accountservice.service.AccountService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public Account createAccount(CreateAccountRequest request) {

        if (!"INR".equals(request.getCurrency())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only INR currency is supported"
            );
        }

        Account account = new Account();

        account.setCustomerId(request.getCustomerId());
        account.setAccountType(request.getAccountType());
        account.setCurrency(request.getCurrency());

        account.setAccountNumber(
                "ACC" + accountRepository.generateAccountNumber()
        );
//        account.setAccountNumber("ACC" + UUID.randomUUID().toString().replace("-", ""));
        account.setBalance(BigDecimal.ZERO);
        account.setAccountStatus(AccountStatus.PENDING_ACTIVATION);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        return accountRepository.save(account);
    }

    @Override
    public Account getAccountById(Long id) {

        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Account ID must be positive"
            );
        }

        return accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account not found with id: " + id
                ));
    }

    @Override
    public Account getByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    @Override
    public List<Account> getMyAccounts(Long customerId) {

        return accountRepository.findByCustomerId(customerId);
    }

    @Override
    public List<Account> getByCustomerId(Long customerId) {

        return accountRepository.findByCustomerId(customerId);
    }

    @Override
    public Page<Account> getAllAccounts(AccountStatus status, AccountType type, Pageable pageable) {

        if (status != null && type != null) {
            return accountRepository.findByAccountStatusAndAccountType(status, type, pageable);
        }

        return accountRepository.findAll(pageable);
    }

    @Override
    public Account updateAccount(Long id, UpdateAccountRequest request) {

        if (request == null || request.getAccountType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account type is required");
        }

        if (request.getCurrency() != null && !"INR".equals(request.getCurrency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only INR currency is supported");
        }

        Account account = getAccountById(id);

        account.setAccountType(request.getAccountType());
        account.setUpdatedAt(LocalDateTime.now());

        return accountRepository.save(account);
    }

    @Override
    public BigDecimal getBalance(Long id) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found"));

        return account.getBalance();
    }

}