package com.banking.accountservice.controller;

import com.banking.accountservice.dto.AccountLifecycleRequest;
import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.AccountStatusHistoryResponse;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatusHistory;
import com.banking.accountservice.service.AccountLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountLifecycleController {

    private final AccountLifecycleService accountLifecycleService;

    @PostMapping("/{accountId}/activate")
    public AccountResponse activateAccount(@PathVariable Long accountId, @RequestBody AccountLifecycleRequest request) {

        return accountLifecycleService.activateAccount(accountId, request.getReason());
    }

    @PostMapping("/{accountId}/freeze")
    public AccountResponse freezeAccount(@PathVariable Long accountId, @RequestBody AccountLifecycleRequest request) {

        return accountLifecycleService.freezeAccount(accountId, request.getReason());
    }

    @PostMapping("/{accountId}/unfreeze")
    public AccountResponse unfreezeAccount(@PathVariable Long accountId, @RequestBody AccountLifecycleRequest request) {

        return accountLifecycleService.unfreezeAccount(accountId, request.getReason());
    }

    @PostMapping("/{accountId}/close")
    public AccountResponse closeAccount(@PathVariable Long accountId, @RequestBody AccountLifecycleRequest request) {

        return accountLifecycleService.closeAccount(accountId, request.getReason());
    }

    @GetMapping("/{accountId}/status-history")
    public List<AccountStatusHistoryResponse> getStatusHistory(@PathVariable Long accountId) {
        return accountLifecycleService.getStatusHistory(accountId);
    }
}
