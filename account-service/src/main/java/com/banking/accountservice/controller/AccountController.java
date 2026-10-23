package com.banking.accountservice.controller;


import com.banking.accountservice.constant.AccountStatus;
import com.banking.accountservice.constant.AccountType;
import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.BalanceResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.dto.UpdateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {

        return accountService.createAccount(request);
    }

    @GetMapping("/{accountId}")
    public AccountResponse getAccountById(@PathVariable Long accountId) {

        return accountService.getAccountById(accountId);
    }

    @GetMapping("/by-number/{accountNumber}")
    public AccountResponse getByAccountNumber(@PathVariable String accountNumber) {

        return accountService.getByAccountNumber(accountNumber);
    }

    @GetMapping("/me")
    public List<AccountResponse> getMyAccounts(@RequestParam Long customerId) {

        return accountService.getMyAccounts(customerId);
    }

    @GetMapping("/customer/{customerId}")
    public List<AccountResponse> getByCustomerId(@PathVariable Long customerId) {

        return accountService.getByCustomerId(customerId);
    }

    @GetMapping
    public Page<AccountResponse> getAllAccounts(@RequestParam(required = false) AccountStatus status, @RequestParam(required = false) AccountType type,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or size");
        }

        return accountService.getAllAccounts(
                status, type, PageRequest.of(page, size));
    }

    @PatchMapping("/{id}")
    public AccountResponse updateAccount(@PathVariable Long id, @RequestBody UpdateAccountRequest request) {

        return accountService.updateAccount(id, request);
    }


    @GetMapping("/{id}/balance")
    public BalanceResponse getBalance(@PathVariable Long id) {

        return accountService.getBalance(id);
    }
}
