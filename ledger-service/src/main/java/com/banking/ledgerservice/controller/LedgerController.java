package com.banking.ledgerservice.controller;

import com.banking.ledgerservice.dto.LedgerEntryResponse;
import com.banking.ledgerservice.dto.LedgerPostingRequest;
import com.banking.ledgerservice.dto.LedgerPostingResponse;
import com.banking.ledgerservice.service.LedgerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/ledger")
@Validated
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping("/entries")
    public ResponseEntity<LedgerPostingResponse> postEntries(@Valid @RequestBody LedgerPostingRequest request) {
        LedgerPostingResponse response = ledgerService.postEntries(request);
        return ResponseEntity.created(URI.create("/ledger/transactions/" + response.transactionId())).body(response);
    }

    @GetMapping("/entries/{entryId}")
    public LedgerEntryResponse getEntryById(@PathVariable @NotNull @Positive Long entryId) {
        return ledgerService.getEntryById(entryId);
    }

}
