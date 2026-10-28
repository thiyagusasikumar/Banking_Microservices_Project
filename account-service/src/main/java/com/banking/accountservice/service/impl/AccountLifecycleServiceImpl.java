package com.banking.accountservice.service.impl;

import com.banking.accountservice.constant.AccountStatus;
import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.AccountStatusHistoryResponse;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatusHistory;
import com.banking.accountservice.mapper.AccountMapper;
import com.banking.accountservice.repository.AccountRepository;
import com.banking.accountservice.repository.AccountStatusHistoryRepository;
import com.banking.accountservice.service.AccountLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountLifecycleServiceImpl implements AccountLifecycleService {

    private final AccountRepository accountRepository;
    private final AccountStatusHistoryRepository historyRepository;

    @Override
    @Transactional
    public AccountResponse activateAccount(Long id, String reason) {
        return changeStatus(id, AccountStatus.PENDING_ACTIVATION, AccountStatus.ACTIVE, reason);
    }

    @Override
    @Transactional
    public AccountResponse freezeAccount(Long id, String reason) {
        return changeStatus(id, AccountStatus.ACTIVE, AccountStatus.FROZEN, reason);
    }

    @Override
    @Transactional
    public AccountResponse unfreezeAccount(Long id, String reason) {
        return changeStatus(id, AccountStatus.FROZEN, AccountStatus.ACTIVE, reason);
    }

    @Override
    @Transactional
    public AccountResponse closeAccount(Long id, String reason) {
        return changeStatus(id, AccountStatus.ACTIVE, AccountStatus.CLOSED, reason);
    }


    @Override
    public List<AccountStatusHistoryResponse> getStatusHistory(Long id) {
        validateId(id);
        if (!accountRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with id: " + id);
        }
        return historyRepository.findByAccountIdOrderByChangedAtDescIdDesc(id).stream()
                .map(history -> new AccountStatusHistoryResponse(
                        history.getId(), history.getAccountId(), history.getPreviousStatus(),
                        history.getNewStatus(), history.getReason(), history.getChangedBy(), history.getChangedAt()))
                .toList();
    }

    private AccountResponse changeStatus(Long id, AccountStatus expected, AccountStatus target, String reason) {
        validateId(id);
        if (reason == null || reason.isBlank() || reason.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status change reason is required and must not exceed 500 characters");
        }
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with id: " + id));
        if (account.getAccountStatus() != expected) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Account must be " + expected + " to change status to " + target);
        }
        if (target == AccountStatus.CLOSED
                && (account.getBalance() == null || account.getBalance().compareTo(BigDecimal.ZERO) != 0)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account balance must be zero to close");
        }

        LocalDateTime now = LocalDateTime.now();
        AccountStatusHistory history = new AccountStatusHistory();
        history.setAccountId(account.getId());
        history.setPreviousStatus(account.getAccountStatus());
        history.setNewStatus(target);
        history.setReason(reason.strip());
        // Replace with the authenticated actor when security is integrated.
        history.setChangedBy("UNAUTHENTICATED");
        history.setChangedAt(now);

        account.setAccountStatus(target);
        account.setUpdatedAt(now);
        Account savedAccount = accountRepository.save(account);
        historyRepository.save(history);
        return AccountMapper.toResponse(savedAccount);
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account ID must be positive");
        }
    }
}
