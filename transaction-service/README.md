# Transaction Service

Implements deposit, withdrawal, transfer, status lookup, idempotency, uncertain outcome handling, reconciliation lookup, Resilience4j protection for Account Service calls, and a database outbox for Kafka `transaction-events`.

## Run

From the parent folder, run `docker compose up -d`. From this folder, run `mvn test` and `mvn spring-boot:run`. Java 21 is required. Default port: 8083. PostgreSQL and Kafka connection settings can be changed with `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, and `ACCOUNT_SERVICE_URL`.

## API

Send a unique `Idempotency-Key` header with each POST. Reusing the key with the same request returns the stored transaction; reusing it with different details returns HTTP 409.

| Method | Path | JSON body |
| --- | --- | --- |
| POST | `/transactions/deposit` | `{ "accountId": "A", "amount": 100.00, "currency": "INR" }` |
| POST | `/transactions/withdrawal` | `{ "accountId": "A", "amount": 20.00, "currency": "INR" }` |
| POST | `/transactions/transfer` | `{ "sourceAccount": "A", "destinationAccount": "B", "amount": 20.00, "currency": "INR" }` |
| GET | `/transactions/{reference}` | None |
| POST | `/transactions/{reference}/reconcile` | None |

For example:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8083/transactions/transfer `
  -Headers @{ 'Idempotency-Key' = 'demo-transfer-001' } `
  -ContentType 'application/json' `
  -Body '{"sourceAccount":"A","destinationAccount":"B","amount":20.00,"currency":"INR"}'
```

## Account Service contract

Transaction Service does **not** hold balances. Account Service must implement:

- `POST /internal/accounts/operations`, with `Idempotency-Key` equal to `transactionReference`. Body: `transactionReference`, `type` (`DEPOSIT`, `WITHDRAWAL`, `TRANSFER`), `sourceAccount`, `destinationAccount`, `amount`, `currency`.
- `GET /internal/accounts/operations/{transactionReference}` to query the final outcome without repeating the money movement.
- Successful response: `{ "status": "APPLIED" }` or `{ "status": "ALREADY_APPLIED" }`. A definite business rejection can respond `{ "status": "REJECTED" }` or HTTP 400/404/409/422. Other responses and connection failures are treated as unknown.
- The POST must be **atomic, durable, and idempotent** for the reference. It must validate account state and balance, lock or version affected accounts, and apply both transfer legs in one database transaction. The lookup must never claim `REJECTED` if an operation might still apply.

Until Account Service implements this contract, POST requests become `PENDING_RECONCILIATION` when its call cannot be completed. Do not retry with a new idempotency key to resolve an unknown outcome. Call the reconcile endpoint after Account Service can answer the lookup.

## Event delivery

The transaction and each event are committed to PostgreSQL together. A scheduled publisher sends pending outbox rows to `transaction-events`; it can redeliver after a crash, so consumers must deduplicate by `eventId`. Events include reference, type, status, amount, currency, and account IDs. Kafka availability does not falsely change a financial transaction's status.

`spring.jpa.hibernate.ddl-auto=update` creates local practice tables, including the unique idempotency key constraint. Use versioned migrations before any deployment. Authentication, approval workflow, ledger posting, and balance processing belong to the separate services and are not active yet.
