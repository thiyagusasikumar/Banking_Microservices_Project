package com.banking.accountservice.controller;

import com.banking.accountservice.dto.AccountLifecycleRequest;
import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.AccountStatusHistoryResponse;
import com.banking.accountservice.service.AccountLifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountLifecycleController {

    private final AccountLifecycleService accountLifecycleService;

    @PostMapping("/{accountId}/activate")
    public ResponseEntity<AccountResponse> activateAccount(@PathVariable Long accountId, @Valid @RequestBody AccountLifecycleRequest request) {

        return ResponseEntity.ok(accountLifecycleService.activateAccount(accountId, request.getReason()));
    }

    @PostMapping("/{accountId}/freeze")
    public ResponseEntity<AccountResponse> freezeAccount(@PathVariable Long accountId, @Valid @RequestBody AccountLifecycleRequest request) {

        return ResponseEntity.ok(accountLifecycleService.freezeAccount(accountId, request.getReason()));
    }

    @PostMapping("/{accountId}/unfreeze")
    public ResponseEntity<AccountResponse> unfreezeAccount(@PathVariable Long accountId, @Valid @RequestBody AccountLifecycleRequest request) {

        return ResponseEntity.ok(accountLifecycleService.unfreezeAccount(accountId, request.getReason()));
    }

    @PostMapping("/{accountId}/close")
    public ResponseEntity<AccountResponse> closeAccount(@PathVariable Long accountId, @Valid @RequestBody AccountLifecycleRequest request) {

        return ResponseEntity.ok(accountLifecycleService.closeAccount(accountId, request.getReason()));
    }

    @GetMapping("/{accountId}/status-history")
    public ResponseEntity<List<AccountStatusHistoryResponse>> getStatusHistory(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountLifecycleService.getStatusHistory(accountId));
    }
}
