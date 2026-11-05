package com.banking.accountservice.service.impl;

import com.banking.accountservice.constant.AccountStatus;
import com.banking.accountservice.constant.AccountType;
import com.banking.accountservice.constant.PostingStatus;
import com.banking.accountservice.dto.AccountOperationRequest;
import com.banking.accountservice.dto.AccountOperationResponse;
import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.BalanceResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.dto.UpdateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountPosting;
import com.banking.accountservice.repository.AccountPostingRepository;
import com.banking.accountservice.repository.AccountRepository;
import com.banking.accountservice.repository.AccountStatusHistoryRepository;

import com.banking.accountservice.service.AccountService;
import com.banking.accountservice.mapper.AccountMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
@Service
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountStatusHistoryRepository historyRepository;
    private final AccountPostingRepository postingRepository;
    private final TransactionTemplate transactionTemplate;

    public AccountServiceImpl(
            AccountRepository accountRepository,
            AccountStatusHistoryRepository historyRepository,
            AccountPostingRepository postingRepository,
            @Qualifier("accountPostingTransactionTemplate") TransactionTemplate transactionTemplate) {
        this.accountRepository = accountRepository;
        this.historyRepository = historyRepository;
        this.postingRepository = postingRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {

        if (request == null || request.getCustomerId() == null || request.getCustomerId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer ID must be positive");
        }
        if (request.getAccountType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account type is required");
        }

        if (!"INR".equals(request.getCurrency())) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only INR currency is supported");
        }

        Account account = new Account();

        account.setCustomerId(request.getCustomerId());
        account.setAccountType(request.getAccountType());
        account.setCurrency(request.getCurrency());

        account.setAccountNumber("ACC" + accountRepository.generateAccountNumber());

        account.setBalance(BigDecimal.ZERO);
        account.setAccountStatus(AccountStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        account.setCreatedAt(now);
        account.setUpdatedAt(now);

        Account savedAccount = accountRepository.save(account);

        return AccountMapper.toResponse(savedAccount);
    }

    @Override
    public AccountResponse getAccountById(Long id) {

        if (id == null || id <= 0) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account ID must be positive");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with id: " + id));

        return AccountMapper.toResponse(account);
    }

    @Override
    public AccountResponse getByAccountNumber(String accountNumber) {

        if (accountNumber == null || accountNumber.isBlank()) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account number is required");
        }

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with number: " + accountNumber));

        return AccountMapper.toResponse(account);
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
                .map(AccountMapper::toResponse)
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

        return accounts.map(AccountMapper::toResponse);
    }

    @Override
    @Transactional
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

        if (account.getAccountStatus() == AccountStatus.CLOSED
                || account.getAccountStatus() == AccountStatus.CLOSURE_PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Closing or closed accounts cannot be updated");
        }

        account.setAccountType(request.getAccountType());
        account.setUpdatedAt(LocalDateTime.now());

        Account savedAccount = accountRepository.save(account);

        return AccountMapper.toResponse(savedAccount);
    }
    @Override
    @Transactional
    public void deleteAccount(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account ID must be positive");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with id: " + id));

        historyRepository.deleteByAccountId(id);
        accountRepository.delete(account);
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

    @Override
    public AccountOperationResponse postOperation(AccountOperationRequest request) {
        AccountOperationRequest normalized = new AccountOperationRequest(
                request.transactionReference(), request.type(), request.sourceAccount(),
                request.destinationAccount(), request.amount(), request.currency().toUpperCase(Locale.ROOT));

        AccountPosting existing = postingRepository.findById(normalized.transactionReference()).orElse(null);
        if (existing != null) {
            return replay(existing, normalized);
        }

        try {
            return transactionTemplate.execute(status -> postOnce(normalized));
        } catch (DataIntegrityViolationException ex) {
            AccountPosting duplicate = postingRepository.findById(normalized.transactionReference())
                    .orElseThrow(() -> ex);
            return replay(duplicate, normalized);
        }
    }

    @Override
    public AccountOperationResponse getOperation(String reference) {
        AccountPosting posting = postingRepository.findById(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Posting not found"));
        return response(posting);
    }

    private AccountOperationResponse postOnce(AccountOperationRequest request) {
        AccountPosting posting = new AccountPosting();
        posting.setTransactionReference(request.transactionReference());
        posting.setType(request.type());
        posting.setSourceAccount(request.sourceAccount());
        posting.setDestinationAccount(request.destinationAccount());
        posting.setAmount(request.amount());
        posting.setCurrency(request.currency());
        posting.setStatus(PostingStatus.REJECTED);
        posting = postingRepository.saveAndFlush(posting);

        switch (request.type()) {
            case DEPOSIT -> deposit(posting);
            case WITHDRAWAL -> withdraw(posting);
            case TRANSFER -> transfer(posting);
        }
        return response(posting);
    }

    private void deposit(AccountPosting posting) {
        Account account = lockedAccount(posting.getDestinationAccount());
        if (account == null) {
            reject(posting, "Destination account not found");
        } else if (!activeAndMatchingCurrency(account, posting)) {
            reject(posting, "Destination account is not ACTIVE or currency does not match");
        } else {
            account.setBalance(account.getBalance().add(posting.getAmount()));
            account.setUpdatedAt(LocalDateTime.now());
            posting.setStatus(PostingStatus.APPLIED);
        }
    }

    private void withdraw(AccountPosting posting) {
        Account account = lockedAccount(posting.getSourceAccount());
        if (account == null) {
            reject(posting, "Source account not found");
        } else if (!activeAndMatchingCurrency(account, posting)) {
            reject(posting, "Source account is not ACTIVE or currency does not match");
        } else if (account.getBalance().compareTo(posting.getAmount()) < 0) {
            reject(posting, "Insufficient balance");
        } else {
            account.setBalance(account.getBalance().subtract(posting.getAmount()));
            account.setUpdatedAt(LocalDateTime.now());
            posting.setStatus(PostingStatus.APPLIED);
        }
    }

    private void transfer(AccountPosting posting) {
        Long sourceId = posting.getSourceAccount();
        Long destinationId = posting.getDestinationAccount();
        if (sourceId == null || destinationId == null || sourceId.equals(destinationId)) {
            reject(posting, "Source and destination must be different accounts");
            return;
        }

        Account first = lockedAccount(Math.min(sourceId, destinationId));
        Account second = lockedAccount(Math.max(sourceId, destinationId));
        if (first == null || second == null) {
            reject(posting, "Source or destination account not found");
            return;
        }
        Account source = sourceId < destinationId ? first : second;
        Account destination = sourceId < destinationId ? second : first;
        if (!activeAndMatchingCurrency(source, posting) || !activeAndMatchingCurrency(destination, posting)) {
            reject(posting, "Source or destination account is not ACTIVE or currency does not match");
        } else if (source.getBalance().compareTo(posting.getAmount()) < 0) {
            reject(posting, "Insufficient balance");
        } else {
            source.setBalance(source.getBalance().subtract(posting.getAmount()));
            destination.setBalance(destination.getBalance().add(posting.getAmount()));
            source.setUpdatedAt(LocalDateTime.now());
            destination.setUpdatedAt(LocalDateTime.now());
            posting.setStatus(PostingStatus.APPLIED);
        }
    }

    private Account lockedAccount(Long id) {
        return id == null ? null : accountRepository.findByIdForUpdate(id).orElse(null);
    }

    private boolean activeAndMatchingCurrency(Account account, AccountPosting posting) {
        return account.getAccountStatus() == AccountStatus.ACTIVE
                && posting.getCurrency().equals(account.getCurrency());
    }

    private void reject(AccountPosting posting, String reason) {
        posting.setStatus(PostingStatus.REJECTED);
        posting.setFailureReason(reason);
    }

    private AccountOperationResponse replay(AccountPosting existing, AccountOperationRequest request) {
        boolean same = existing.getType() == request.type()
                && Objects.equals(existing.getSourceAccount(), request.sourceAccount())
                && Objects.equals(existing.getDestinationAccount(), request.destinationAccount())
                && existing.getAmount().compareTo(request.amount()) == 0
                && existing.getCurrency().equals(request.currency());
        if (!same) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transaction reference was already used for a different operation");
        }
        return response(existing);
    }

    private AccountOperationResponse response(AccountPosting posting) {
        return new AccountOperationResponse(posting.getTransactionReference(), posting.getStatus(),
                posting.getFailureReason());
    }
}
