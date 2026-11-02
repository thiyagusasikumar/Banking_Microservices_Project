package com.banking.accountservice.service;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.AccountStatusHistoryResponse;

import java.util.List;

public interface AccountLifecycleService {

    AccountResponse activateAccount(Long id, String reason, String changedBy);

    AccountResponse freezeAccount(Long id, String reason, String changedBy);

    AccountResponse unfreezeAccount(Long id, String reason, String changedBy);

    AccountResponse closeAccount(Long id, String reason, String changedBy);

    List<AccountStatusHistoryResponse> getStatusHistory(Long id);
}
