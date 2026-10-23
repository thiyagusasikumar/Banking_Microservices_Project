package com.banking.accountservice.service.impl;

import com.banking.accountservice.constant.AccountStatus;
import com.banking.accountservice.constant.AccountType;
import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.BalanceResponse;
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
    public AccountResponse createAccount(CreateAccountRequest request) {

        if (!"INR".equals(request.getCurrency())) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only INR currency is supported");
        }

        Account account = new Account();

        account.setCustomerId(request.getCustomerId());
        account.setAccountType(request.getAccountType());
        account.setCurrency(request.getCurrency());

        account.setAccountNumber("ACC" + accountRepository.generateAccountNumber());

        account.setBalance(BigDecimal.ZERO);
        account.setAccountStatus(AccountStatus.PENDING_ACTIVATION);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        Account savedAccount = accountRepository.save(account);

        return new AccountResponse(
                savedAccount.getId(),
                savedAccount.getCustomerId(),
                savedAccount.getAccountNumber(),
                savedAccount.getAccountType(),
                savedAccount.getBalance(),
                savedAccount.getCurrency(),
                savedAccount.getAccountStatus(),
                savedAccount.getCreatedAt(),
                savedAccount.getUpdatedAt()
        );
    }

    @Override
    public AccountResponse getAccountById(Long id) {

        if (id == null || id <= 0) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account ID must be positive");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with id: " + id));

        return new AccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency(),
                account.getAccountStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    @Override
    public AccountResponse getByAccountNumber(String accountNumber) {

        if (accountNumber == null || accountNumber.isBlank()) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account number is required");
        }

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with number: " + accountNumber));

        return new AccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency(),
                account.getAccountStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    @Override
    public List<AccountResponse> getMyAccounts(Long customerId) {

        return getByCustomerId(customerId);
    }

    @Override
    public List<AccountResponse> getByCustomerId(Long customerId) {

        if (customerId == null || customerId <= 0) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer ID must be positive");
        }

        List<Account> accounts = accountRepository.findByCustomerId(customerId);

        return accounts.stream()
                .map(account -> new AccountResponse(
                        account.getId(),
                        account.getCustomerId(),
                        account.getAccountNumber(),
                        account.getAccountType(),
                        account.getBalance(),
                        account.getCurrency(),
                        account.getAccountStatus(),
                        account.getCreatedAt(),
                        account.getUpdatedAt()
                ))
                .toList();
    }

    @Override
    public Page<AccountResponse> getAllAccounts(AccountStatus status, AccountType type, Pageable pageable) {

        Page<Account> accounts;

        if (status != null && type != null) {
            accounts = accountRepository.findByAccountStatusAndAccountType(status, type, pageable);

        } else if (status != null) {
            accounts = accountRepository.findByAccountStatus(status, pageable);

        } else if (type != null) {
            accounts = accountRepository.findByAccountType(type, pageable);

        } else {
            accounts = accountRepository.findAll(pageable);
        }

        return accounts.map(account -> new AccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency(),
                account.getAccountStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        ));
    }

    @Override
    public AccountResponse updateAccount(Long id, UpdateAccountRequest request) {

        if (id == null || id <= 0) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account ID must be positive");
        }

        if (request == null || request.getAccountType() == null) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account type is required");
        }

        if (request.getCurrency() != null && !"INR".equals(request.getCurrency())) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only INR currency is supported");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with id: " + id));

        account.setAccountType(request.getAccountType());
        account.setUpdatedAt(LocalDateTime.now());

        Account savedAccount = accountRepository.save(account);

        return new AccountResponse(
                savedAccount.getId(),
                savedAccount.getCustomerId(),
                savedAccount.getAccountNumber(),
                savedAccount.getAccountType(),
                savedAccount.getBalance(),
                savedAccount.getCurrency(),
                savedAccount.getAccountStatus(),
                savedAccount.getCreatedAt(),
                savedAccount.getUpdatedAt()
        );
    }
    @Override
    public BalanceResponse getBalance(Long id) {

        if (id == null || id <= 0) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account ID must be positive");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with id: " + id));

        return new BalanceResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency()
        );
    }

}