package com.banking.accountservice.controller;

import com.banking.accountservice.dto.AccountLifecycleRequest;
import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.AccountStatusHistoryResponse;
import com.banking.accountservice.service.AccountLifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountLifecycleController {

    private final AccountLifecycleService accountLifecycleService;

    @PostMapping("/{accountId}/activate")
    public ResponseEntity<AccountResponse> activateAccount(@PathVariable Long accountId,
                                                           @AuthenticationPrincipal Jwt reviewer,
                                                           @Valid @RequestBody AccountLifecycleRequest request) {

        return ResponseEntity.ok(accountLifecycleService.activateAccount(accountId, request.getReason(), reviewer.getSubject()));
    }

    @PostMapping("/{accountId}/freeze")
    public ResponseEntity<AccountResponse> freezeAccount(@PathVariable Long accountId,
                                                         @AuthenticationPrincipal Jwt reviewer,
                                                         @Valid @RequestBody AccountLifecycleRequest request) {

        return ResponseEntity.ok(accountLifecycleService.freezeAccount(accountId, request.getReason(), reviewer.getSubject()));
    }

    @PostMapping("/{accountId}/unfreeze")
    public ResponseEntity<AccountResponse> unfreezeAccount(@PathVariable Long accountId,
                                                           @AuthenticationPrincipal Jwt reviewer,
                                                           @Valid @RequestBody AccountLifecycleRequest request) {

        return ResponseEntity.ok(accountLifecycleService.unfreezeAccount(accountId, request.getReason(), reviewer.getSubject()));
    }

    @PostMapping("/{accountId}/close")
    public ResponseEntity<AccountResponse> closeAccount(@PathVariable Long accountId,
                                                        @AuthenticationPrincipal Jwt reviewer,
                                                        @Valid @RequestBody AccountLifecycleRequest request) {

        return ResponseEntity.ok(accountLifecycleService.closeAccount(accountId, request.getReason(), reviewer.getSubject()));
    }

    @GetMapping("/{accountId}/status-history")
    public ResponseEntity<List<AccountStatusHistoryResponse>> getStatusHistory(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountLifecycleService.getStatusHistory(accountId));
    }
}
