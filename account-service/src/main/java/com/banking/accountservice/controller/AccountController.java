package com.banking.accountservice.controller;


import com.banking.accountservice.constant.AccountStatus;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account createAccount(@Valid @RequestBody CreateAccountRequest request){
        return accountService.createAccount(request);
    }

    @GetMapping("/{id}")
    public Account getAccountById(@PathVariable Long id){
        return accountService.getAccountById(id);
    }

    @GetMapping("/getAll")
    public List<Account> getAllAccount() {
        return accountService.getAllAccount();
    }


    @GetMapping("/{id}/balance")
    public BigDecimal getBalance(@PathVariable Long id) {
        return accountService.getBalance(id);
    }

    @GetMapping("/customer/{customerId}")
    public List<Account> getAccountByCustomerId(@PathVariable Long customerId){
        return accountService.getAccountByCustomerId(customerId);
    }


    @PutMapping("/{id}/status")
    public Account updateStatus(@PathVariable Long id, @RequestParam AccountStatus status){
        return accountService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public String deleteAccount(@PathVariable Long id){
       return accountService.deleteAccount(id);
    }
}
