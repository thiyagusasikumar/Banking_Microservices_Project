package com.banking.transactionservice;

import java.math.BigDecimal;

public interface AccountClient {
    Outcome apply(String reference, TransactionRecord.Type type, String sourceAccount,
                  String destinationAccount, BigDecimal amount, String currency);
    Outcome lookup(String reference);

    enum Outcome { APPLIED, REJECTED, UNKNOWN }
}
