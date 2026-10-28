package com.banking.ledgerservice.service.impl;

import com.banking.ledgerservice.constant.EntryType;
import com.banking.ledgerservice.dto.*;
import com.banking.ledgerservice.entity.LedgerEntry;
import com.banking.ledgerservice.exception.DuplicateLedgerEntryException;
import com.banking.ledgerservice.exception.InvalidLedgerEntryException;
import com.banking.ledgerservice.exception.LedgerEntryNotFoundException;
import com.banking.ledgerservice.mapper.LedgerEntryMapper;
import com.banking.ledgerservice.repository.LedgerEntryRepository;
import com.banking.ledgerservice.service.LedgerService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import java.util.UUID;

@Service
public class LedgerServiceImpl implements LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerEntryMapper ledgerEntryMapper;

    public LedgerServiceImpl(LedgerEntryRepository ledgerEntryRepository, LedgerEntryMapper ledgerEntryMapper) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.ledgerEntryMapper = ledgerEntryMapper;
    }

    @Override
    @Transactional
    public LedgerPostingResponse postEntries(LedgerPostingRequest request) {
        ValidatedPosting posting = validateAndNormalize(request);

        if (hasExistingPosting(posting)) {
            throw duplicatePosting(posting.transactionId());
        }

        List<LedgerEntry> entries = List.of(
                newEntry(posting.transactionId(), posting.debitAccountId(), EntryType.DEBIT, posting.amount(), posting.currency()),
                newEntry(posting.transactionId(), posting.creditAccountId(), EntryType.CREDIT, posting.amount(), posting.currency())
        );

        try {
            List<LedgerEntryResponse> responses = ledgerEntryRepository.saveAllAndFlush(entries).stream()
                    .map(ledgerEntryMapper::toResponse)
                    .toList();
            return new LedgerPostingResponse(posting.transactionId(), responses);
        } catch (DataIntegrityViolationException exception) {
            throw duplicatePosting(posting.transactionId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerEntryResponse getEntryById(Long entryId) {
        validatePositiveId(entryId, "Ledger entry ID");
        return ledgerEntryRepository.findById(entryId)
                .map(ledgerEntryMapper::toResponse)
                .orElseThrow(() -> new LedgerEntryNotFoundException(entryId));
    }



    private boolean hasExistingPosting(ValidatedPosting posting) {
        return ledgerEntryRepository.existsByTransactionId(posting.transactionId());
    }

    private LedgerEntry newEntry(UUID transactionId, Long accountId, EntryType entryType,
                                 BigDecimal amount, String currency) {
        LedgerEntry entry = new LedgerEntry();
        entry.setTransactionId(transactionId);
        entry.setAccountId(accountId);
        entry.setEntryType(entryType);
        entry.setAmount(amount);
        entry.setCurrency(currency);
        return entry;
    }

    private ValidatedPosting validateAndNormalize(LedgerPostingRequest request) {
        if (request == null) {
            throw new InvalidLedgerEntryException("Ledger posting request is required");
        }
        if (request.transactionId() == null) {
            throw new InvalidLedgerEntryException("Transaction ID is required");
        }
        validatePositiveId(request.debitAccountId(), "Debit account ID");
        validatePositiveId(request.creditAccountId(), "Credit account ID");
        if (request.debitAccountId().equals(request.creditAccountId())) {
            throw new InvalidLedgerEntryException("Debit and credit account IDs must be different");
        }
        if (request.amount() == null || request.amount().signum() <= 0 || request.amount().scale() > 2
                || request.amount().precision() - request.amount().scale() > 17) {
            throw new InvalidLedgerEntryException("Amount must be positive with at most 17 whole digits and 2 decimal places");
        }

        return new ValidatedPosting(
                request.transactionId(),
                request.debitAccountId(),
                request.creditAccountId(),
                request.amount().setScale(2, RoundingMode.UNNECESSARY),
                normalizeCurrency(request.currency())
        );
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || !currency.matches("[A-Za-z]{3}")) {
            throw new InvalidLedgerEntryException("Currency must be a valid three-letter code");
        }
        String normalized = currency.toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(normalized);
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new InvalidLedgerEntryException("Currency must be a valid ISO 4217 code");
        }
    }

    private void validatePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new InvalidLedgerEntryException(fieldName + " must be positive");
        }
    }

    private DuplicateLedgerEntryException duplicatePosting(UUID transactionId) {
        return new DuplicateLedgerEntryException("Ledger entries have already been posted for transaction: " + transactionId);
    }

}
