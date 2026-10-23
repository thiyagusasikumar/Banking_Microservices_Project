package com.banking.accountservice.service;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.AccountStatusHistoryResponse;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatusHistory;

import java.util.List;

public interface AccountLifecycleService {

    AccountResponse activateAccount(Long id, String reason);

    AccountResponse freezeAccount(Long id, String reason);

    AccountResponse unfreezeAccount(Long id, String reason);

    AccountResponse closeAccount(Long id, String reason);

    List<AccountStatusHistoryResponse> getStatusHistory(Long id);
}
