-- Run in the account-service PostgreSQL database (baccount_db).
-- Existing rows must use one of the statuses below. If validation fails,
-- the transaction rolls back; review legacy statuses before retrying.
BEGIN;

ALTER TABLE accounts
    DROP CONSTRAINT IF EXISTS accounts_account_status_check,
    DROP CONSTRAINT IF EXISTS chk_account_status;

ALTER TABLE accounts
    ADD CONSTRAINT accounts_account_status_check
    CHECK (account_status IN (
        'PENDING_ACTIVATION',
        'ACTIVE',
        'FROZEN',
        'RESTRICTED',
        'CLOSURE_PENDING',
        'CLOSED'
    ));

COMMIT;
