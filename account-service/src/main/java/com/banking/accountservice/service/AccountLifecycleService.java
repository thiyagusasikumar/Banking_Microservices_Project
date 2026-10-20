package com.banking.accountservice.service;

import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatusHistory;

import java.util.List;

public interface AccountLifecycleService {

    Account activateAccount(Long id, String reason);

    Account freezeAccount(Long id, String reason);

    Account unfreezeAccount(Long id, String reason);

    Account closeAccount(Long id, String reason);

    List<AccountStatusHistory> getStatusHistory(Long id);
}
