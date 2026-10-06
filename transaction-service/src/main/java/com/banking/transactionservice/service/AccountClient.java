package com.banking.transactionservice.service;

import com.banking.transactionservice.entity.TransactionRecord;

import java.math.BigDecimal;

public interface AccountClient {
    Outcome apply(String reference, TransactionRecord.Type type, String sourceAccount,
                  String destinationAccount, BigDecimal amount, String currency);
    Outcome lookup(String reference);

    enum Outcome { APPLIED, REJECTED, UNKNOWN }
}
