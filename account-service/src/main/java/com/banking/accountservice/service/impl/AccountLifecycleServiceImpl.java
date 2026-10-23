package com.banking.accountservice.service.impl;

import com.banking.accountservice.constant.AccountStatus;
import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.AccountStatusHistoryResponse;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatusHistory;
import com.banking.accountservice.repository.AccountRepository;
import com.banking.accountservice.repository.AccountStatusHistoryRepository;
import com.banking.accountservice.service.AccountLifecycleService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountLifecycleServiceImpl implements AccountLifecycleService {

    private final AccountRepository accountRepository;
    private final AccountStatusHistoryRepository historyRepository;

    @Override
    @Transactional
    public AccountResponse activateAccount(Long id, String reason) {

        if (id == null || id <= 0) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid account ID");
        }

        if (reason == null || reason.isBlank()) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activation reason is required");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        if (account.getAccountStatus() != AccountStatus.PENDING_ACTIVATION) {

            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending accounts can be activated");
        }

        Account savedAccount = changeStatus(id, AccountStatus.ACTIVE, reason);

        return toResponse(savedAccount);
    }

    @Override
    @Transactional
    public AccountResponse freezeAccount(Long id, String reason) {

        if (id == null || id <= 0) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid account ID");
        }

        if (reason == null || reason.isBlank()) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Freeze reason is required");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        if (account.getAccountStatus() != AccountStatus.ACTIVE) {

            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active accounts can be frozen");
        }

        Account savedAccount = changeStatus(id, AccountStatus.FROZEN, reason);

        return toResponse(savedAccount);
    }

    @Override
    @Transactional
    public AccountResponse unfreezeAccount(Long id, String reason) {

        if (id == null || id <= 0) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid account ID");
        }

        if (reason == null || reason.isBlank()) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unfreeze reason is required");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        if (account.getAccountStatus() != AccountStatus.FROZEN) {

            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only frozen accounts can be unfrozen");
        }

        Account savedAccount = changeStatus(id, AccountStatus.ACTIVE, reason);

        return toResponse(savedAccount);
    }

    @Override
    @Transactional
    public AccountResponse closeAccount(Long id, String reason) {

        if (id == null || id <= 0) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid account ID");
        }

        if (reason == null || reason.isBlank()) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Closure reason is required");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        if (account.getAccountStatus() != AccountStatus.ACTIVE) {

            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active accounts can be closed");
        }

        if (account.getBalance() == null ||
                account.getBalance().compareTo(BigDecimal.ZERO) != 0) {

            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account balance must be zero to close");
        }

        Account savedAccount = changeStatus(id, AccountStatus.CLOSED, reason);

        return toResponse(savedAccount);
    }

    @Override
    public List<AccountStatusHistoryResponse> getStatusHistory(Long id) {

        if (id == null || id <= 0) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account ID must be positive");
        }

        if (!accountRepository.existsById(id)) {

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with id: " + id);
        }

        List<AccountStatusHistory> history = historyRepository.findByAccountIdOrderByChangedAtDescIdDesc(id);

        return history.stream()
                .map(status -> new AccountStatusHistoryResponse(
                        status.getId(),
                        status.getAccountId(),
                        status.getPreviousStatus(),
                        status.getNewStatus(),
                        status.getReason(),
                        status.getChangedBy(),
                        status.getChangedAt()
                ))
                .toList();
    }

    private Account changeStatus(Long id, AccountStatus target, String reason) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with id: " + id));

        LocalDateTime now = LocalDateTime.now();

        AccountStatusHistory history = new AccountStatusHistory();
        history.setAccountId(account.getId());
        history.setPreviousStatus(account.getAccountStatus());
        history.setNewStatus(target);
        history.setReason(reason);
        history.setChangedBy("UNAUTHENTICATED");
        history.setChangedAt(now);

        account.setAccountStatus(target);
        account.setUpdatedAt(now);

        Account savedAccount = accountRepository.save(account);
        historyRepository.save(history);

        return savedAccount;
    }
    private AccountResponse toResponse(Account account) {

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
}
