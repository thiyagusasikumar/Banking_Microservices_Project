package com.banking.accountservice.controller;


import com.banking.accountservice.constant.AccountStatus;
import com.banking.accountservice.constant.AccountType;
import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.BalanceResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.dto.UpdateAccountRequest;
import com.banking.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

//    @PostMapping
//    public ResponseEntity<AccountResponse> createAccount(
//            @Valid @RequestBody CreateAccountRequest request,
//            @AuthenticationPrincipal  Jwt jwt) {
//        // Ignore any customerId from the client; bind ownership to the authenticated customer.
//        request.setCustomerId(getCustomerId(jwt));
//        AccountResponse response = accountService.createAccount(request);
//        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
//                .path("/{accountId}").buildAndExpand(response.getId()).toUri()).body(response);
//    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request, @AuthenticationPrincipal Jwt jwt) {

        request.setCustomerId(getCustomerId(jwt));

        AccountResponse response = accountService.createAccount(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long accountId) {

        return ResponseEntity.ok(accountService.getAccountById(accountId));
    }

    @GetMapping("/by-number/{accountNumber}")
    public ResponseEntity<AccountResponse> getByAccountNumber(@PathVariable String accountNumber) {

        return ResponseEntity.ok(accountService.getByAccountNumber(accountNumber));
    }

    @GetMapping("/me")
    public ResponseEntity<List<AccountResponse>> getMyAccounts(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(accountService.getMyAccounts(getCustomerId(jwt)));
    }

    @GetMapping("/me/customer-id")
    public ResponseEntity<java.util.Map<String, Long>> getCurrentCustomerId(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(java.util.Map.of("customerId", getCustomerId(jwt)));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountResponse>> getByCustomerId(@PathVariable Long customerId) {

        return ResponseEntity.ok(accountService.getByCustomerId(customerId));
    }

    @GetMapping
    public ResponseEntity<Page<AccountResponse>> getAllAccounts(@RequestParam(required = false) AccountStatus status, @RequestParam(required = false) AccountType type,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or size");
        }

        return ResponseEntity.ok(accountService.getAllAccounts(
                status, type, PageRequest.of(page, size)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(@PathVariable Long id, @Valid @RequestBody UpdateAccountRequest request) {

        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }


    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable Long id) {

        return ResponseEntity.ok(accountService.getBalance(id));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long accountId) {
        accountService.deleteAccount(accountId);
        return ResponseEntity.noContent().build();
    }

    private Long getCustomerId(Jwt jwt) {
        String customerIdClaim = jwt.getClaimAsString("customer_id");
        try {
            long customerId = Long.parseLong(customerIdClaim);
            if (customerId <= 0) {
                throw new NumberFormatException("customer_id must be positive");
            }
            return customerId;
        } catch (NumberFormatException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Access token is missing a valid customer_id claim", ex);
        }
    }
}
