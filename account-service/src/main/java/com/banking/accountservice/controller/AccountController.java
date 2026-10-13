package com.banking.accountservice.controller;


import com.banking.accountservice.constant.AccountStatus;
import com.banking.accountservice.constant.AccountType;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account createAccount(@Valid @RequestBody CreateAccountRequest request){
        return accountService.createAccount(request);
    }

    @GetMapping("/{accountId}")
    public Account getAccountById(@PathVariable("accountId") Long id){
        return accountService.getAccountById(id);
    }

    @GetMapping("/by-number/{accountNumber}")
    public Account getByAccountNumber(@PathVariable String accountNumber){
        return accountService.getByAccountNumber(accountNumber);
    }

    @GetMapping("/me")
    public List<Account> getMyAccounts(@RequestParam Long customerId) {

        return accountService.getMyAccounts(customerId);
    }

    @GetMapping("/customer/{customerId}")
    public List<Account> getByCustomerId(@PathVariable Long customerId) {

        return accountService.getByCustomerId(customerId);
    }

    @GetMapping
    public Page<Account> getAllAccounts(@RequestParam(required = false) AccountStatus status, @RequestParam(required = false) AccountType type,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

        return accountService.getAllAccounts(
                status, type, PageRequest.of(page, size));
    }

    @PatchMapping("/{id}")
    public Account updateAccount(@PathVariable Long id, @RequestBody UpdateAccountRequest request) {
        return accountService.updateAccount(id, request);
    }

    @GetMapping("/{id}/balance")
    public BigDecimal getBalance(@PathVariable Long id) {

        return accountService.getBalance(id);
    }
}
