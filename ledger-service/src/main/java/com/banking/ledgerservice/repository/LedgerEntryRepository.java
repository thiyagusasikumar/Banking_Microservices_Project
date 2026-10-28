package com.banking.ledgerservice.repository;

import com.banking.ledgerservice.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    boolean existsByTransactionId(UUID transactionId);

}
