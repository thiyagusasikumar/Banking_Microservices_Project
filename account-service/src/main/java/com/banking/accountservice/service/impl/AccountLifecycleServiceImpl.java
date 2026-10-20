package com.banking.accountservice.service.impl;

import com.banking.accountservice.constant.AccountStatus;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatusHistory;
import com.banking.accountservice.repository.AccountRepository;
import com.banking.accountservice.repository.AccountStatusHistoryRepository;
import com.banking.accountservice.service.AccountLifecycleService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountLifecycleServiceImpl implements AccountLifecycleService {

    private final AccountRepository accountRepository;
    private final AccountStatusHistoryRepository historyRepository;

    @Override
    @Transactional
    public Account activateAccount(Long id, String reason) {
        return changeStatus(id, AccountStatus.ACTIVE, reason);
    }

    @Override
    @Transactional
    public Account freezeAccount(Long id, String reason) {
        return changeStatus(id, AccountStatus.FROZEN, reason);
    }

    @Override
    @Transactional
    public Account unfreezeAccount(Long id, String reason) {
        return changeStatus(id, AccountStatus.ACTIVE, reason);
    }

    @Override
    @Transactional
    public Account closeAccount(Long id, String reason) {
        return changeStatus(id, AccountStatus.CLOSED, reason);
    }

    @Override
    public List<AccountStatusHistory> getStatusHistory(Long id)  {
        if (!accountRepository.existsById(id)) {
            return null;
        }
        return historyRepository.findByAccountIdOrderByChangedAtDescIdDesc(id);
    }

    private Account changeStatus(Long id, AccountStatus target, String reason) {
        Account account = accountRepository.findById(id).orElse(null);
        if (account == null) {
            return null;
        }
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
        Account saved = accountRepository.save(account);
        historyRepository.save(history);
        return saved;
    }
}
