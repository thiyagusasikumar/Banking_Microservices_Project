# KYC workflow (starter implementation)

Account Service persists KYC applications in its own database. A customer submits a private document-storage reference; an authorized staff member reviews the application manually. Account creation is blocked until at least one application for that customer is approved.

## API

All requests use `Authorization: Bearer <access-token>`. The access token must contain a positive numeric `customer_id` claim for customer endpoints. Staff review endpoints require a Keycloak realm role of `CHECKER`, `MARKER`, or `ADMIN`.

- `POST /api/v1/kyc/me` submits an application. JSON fields: `documentType` and `documentReference` (an object key/reference in private storage). Customer ID comes from the token.
- `GET /api/v1/kyc/me` returns the customer's newest application and decision.
- `GET /api/v1/kyc/reviews/pending` lists pending applications for staff.
- `PATCH /api/v1/kyc/reviews/{applicationId}` reviews a pending application. JSON fields: `decision` (`APPROVED`, `REJECTED`, or `NEEDS_MORE_INFO`) and optional `note`. A rejection requires a reason.
- `POST /api/v1/accounts` returns `403` until KYC is approved.

Example submission:

```http
POST /api/v1/kyc/me
Authorization: Bearer <access-token>
Content-Type: application/json

{"documentType":"PASSPORT","documentReference":"private-kyc/opaque-object-key"}
```

The API stores only a reference, not the uploaded document. Document upload, encryption/storage policy, antivirus scanning, automated checks, retention/deletion rules, and production KYC requirements still need to be selected and implemented before using real customer documents. Use fake document references while developing.
