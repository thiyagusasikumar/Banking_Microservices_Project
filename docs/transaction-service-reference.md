# Transaction Service Technical Reference

This document describes the Transaction Service implementation recorded in Git commit `54d378e` (`Organize transaction service into layers`). The service is **not present in the current `main` branch**; later commits removed its folder. The behavior below is a reference for understanding or restoring that version, not a claim that the current repository can run it.

## Purpose and boundaries

The service accepts deposits, withdrawals, and transfers; stores a transaction reference and status; prevents duplicate execution for a repeated idempotency key; requests an account operation; and publishes transaction events through a PostgreSQL outbox. It does not store account balances or debit and credit accounts itself. The earlier implementation expected Account Service to make the actual balance change atomically and idempotently.

The code used Java 21, Spring Boot 3.5.16, Spring Web, Data JPA, Bean Validation, PostgreSQL, Spring Kafka, Resilience4j 2.4.0, and H2 for tests. The historical application defaulted to port `8083`.

## Package structure

Under `com.banking.transactionservice`:

| Package | Main classes | Responsibility |
| --- | --- | --- |
| `controller` | `TransactionController` | REST endpoints and HTTP responses |
| `service` | `TransactionService`, `AccountClient`, `HttpAccountClient`, `OutboxPublisher` | Transaction flow, Account Service integration, event publishing |
| `repository` | `TransactionRepository`, `OutboxRepository` | JPA access |
| `entity` | `TransactionRecord`, `OutboxEvent` | PostgreSQL records |
| `dto` | Deposit, withdrawal, transfer, transaction, and account operation records | API and integration payloads |
| `exception` | Invalid transaction, idempotency conflict, not found, global handler | Error responses |
| `config` | `AccountClientConfig` | Account Service HTTP client and timeouts |
| `mapper` | `TransactionMapper` | API response and event payload mapping |

`TransactionServiceApplication` lived at the package root and enabled scheduling for the outbox publisher.

## REST API

Every creation request required an `Idempotency-Key` header. Request amounts had to be positive, use at most two decimal places, and fit `NUMERIC(19,2)`. Currency used a three-letter code, normalized to uppercase. A transfer could not use the same source and destination account.

| Method | Path | Request body | Result |
| --- | --- | --- | --- |
| `POST` | `/transactions/deposit` | `accountId`, `amount`, `currency` | Creates a deposit; HTTP 201 |
| `POST` | `/transactions/withdrawal` | `accountId`, `amount`, `currency` | Creates a withdrawal; HTTP 201 |
| `POST` | `/transactions/transfer` | `sourceAccount`, `destinationAccount`, `amount`, `currency` | Creates a transfer; HTTP 201 |
| `GET` | `/transactions/{reference}` | None | Returns stored transaction; HTTP 200 |
| `POST` | `/transactions/{reference}/reconcile` | None | Queries Account Service for an uncertain outcome; HTTP 200 |

Example transfer request:

```http
POST /transactions/transfer
Idempotency-Key: transfer-demo-001
Content-Type: application/json

{"sourceAccount":"A","destinationAccount":"B","amount":100.00,"currency":"INR"}
```

The response includes `reference`, `type`, `status`, source and destination account identifiers, `amount`, `currency`, `failureReason`, `createdAt`, and `updatedAt`. The create endpoint returns HTTP 201 even when the same key and identical request return an existing transaction. A reused key with different request details returns HTTP 409. Invalid input returns HTTP 400; an unknown reference returns HTTP 404.

## Transaction states and processing

| State | Meaning | Next action |
| --- | --- | --- |
| `INITIATED` | Transaction and creation event have been stored; the account result has not been recorded | Inspect or reconcile by reference |
| `COMPLETED` | Account Service confirmed `APPLIED` or `ALREADY_APPLIED` | No new money movement |
| `FAILED` | Account Service gave a definite rejection | Correct the business request before a new transaction |
| `PENDING_RECONCILIATION` | Timeout, unavailable service, or otherwise unknown account result | Query the existing reference; do not retry with a new key |

The creation path validates and normalizes the request, hashes its significant fields, and inserts `INITIATED` plus a `TransactionCreated` outbox row in one database transaction. It then calls Account Service once. `APPLIED` moves the record to `COMPLETED`; a definite `REJECTED` result moves it to `FAILED`; an unknown result moves it to `PENDING_RECONCILIATION`. Each transition writes its event to the outbox in the same local transaction as the status change.

The reconcile endpoint queries Account Service using the original transaction reference. A confirmed applied or rejected result closes the pending transaction and adds a completion or failure event. An unknown lookup leaves the stored status unchanged. The POST that moves money was not automatically retried; only the read-only lookup had three attempts with exponential backoff.

## Persistence and idempotency

`transactions` stored a UUID ID, unique transaction reference, account identifiers, amount, currency, type, status, unique `idempotency_key`, SHA-256 request hash, optional failure reason, timestamps, and an optimistic-lock version. The uniqueness constraint on `idempotency_key` was the final duplicate protection. An identical repeated request returned the stored record without calling Account Service again; a different request using the key raised an idempotency conflict.

`transaction_outbox` stored event ID, transaction reference, event type, JSON payload, creation time, and publication time. The scheduled publisher sent up to 50 unpublished rows per pass to Kafka topic `transaction-events`, then marked them published. A crash after sending but before marking could produce a duplicate event. Consumers therefore need to deduplicate by the event payload's `eventId`.

The historical local configuration used `spring.jpa.hibernate.ddl-auto=update`; a production deployment would need reviewed, versioned migrations. The schema and application were not verified against a live PostgreSQL/Kafka stack in the recorded test run.

## Account Service contract

The historical Transaction Service expected:

1. `POST /internal/accounts/operations` with header `Idempotency-Key: {transactionReference}` and a body containing `transactionReference`, `type`, source and destination account identifiers, amount, and currency.
2. `GET /internal/accounts/operations/{transactionReference}` to return the final result without repeating the movement.
3. A response status of `APPLIED`, `ALREADY_APPLIED`, or `REJECTED`. HTTP 400, 404, 409, and 422 on the POST were treated as definite rejection; other non-success responses and connection errors were treated as unknown.

For this contract to be safe, Account Service must perform each requested operation atomically and durably, deduplicate by transaction reference, and make the lookup authoritative. A transfer must debit and credit in one Account Service database transaction. Its lookup must not report rejection while the operation may still apply. The current repository's Account Service remains a starter project and does not implement this contract.

## Configuration and local use

The historical service read `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `ACCOUNT_SERVICE_URL`, and `SERVER_PORT`. Defaults targeted PostgreSQL at `localhost:5432/transaction_db`, Kafka at `localhost:9092`, Account Service at `localhost:8082`, and Transaction Service at `localhost:8083`. The root `compose.yaml` currently defines PostgreSQL and Kafka. That Compose file does not supply the missing Account Service operation behavior.

Resilience4j circuit breakers and semaphore bulkheads guarded Account Service POST and lookup calls. HTTP connect and read timeouts were 2 and 5 seconds. The lookup had up to three attempts; the money-moving POST had one attempt. No fallback reported a financial operation as successful.

## Verification and current status

At commit `54d378e`, seven tests passed: one generated application-context test and six tests covering an idempotent completed transfer, a reused key conflict, unknown-result reconciliation, invalid same-account transfer, HTTP create/read, and HTTP error statuses. These tests used H2 and a mocked Account Service client. Live PostgreSQL, Kafka, and Account Service integration were not tested in that run.

The current `main` branch does not contain `transaction-service`; this document preserves the earlier design and behavior. Authentication, maker-checker approval, ledger posting, notifications, and end-to-end account balance changes were outside that version's implementation.
