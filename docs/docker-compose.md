# Running the banking stack in Docker

All nine Spring applications are defined in the root `compose.yaml`, together
with two PostgreSQL instances, Keycloak, Kafka, and Mailpit: 14 services in total.
The application images build from source using the shared multi-stage
`Dockerfile`, Maven, and Java 21. Java does not need to be installed on the host
for Docker builds. The runtime images run as an unprivileged user.

## Start, inspect, and stop

Run these commands from `D:\Banking_Microservices_Project`:

```powershell
docker compose up -d --build --wait --wait-timeout 240
docker compose ps
.\scripts\Test-Gateway.ps1
docker compose logs --tail 100 api-gateway eureka-server
docker compose logs -f auth-service
docker compose stop
```

After the images have been built, `docker compose up -d --wait --wait-timeout 240`
starts the stack without rebuilding. To rebuild one changed service:

```powershell
docker compose up -d --build --no-deps auth-service
```

The old `scripts/Start-Banking.ps1` and `scripts/Stop-Banking.ps1` manage only
Windows Java processes. They are not used to start or stop Docker containers.
Those Windows processes were stopped during this migration. The unrelated
`oms-postgres` container is outside this Compose project and is unchanged.

## Addresses

| Component | Host address | Docker network address |
| --- | --- | --- |
| Gateway | `http://localhost:8090` | `http://api-gateway:8090` |
| Eureka dashboard | `http://localhost:8761` | `http://eureka-server:8761` |
| Keycloak | `http://localhost:8080` | `http://keycloak:8080` |
| Auth PostgreSQL | `localhost:5433` | `auth-db:5432` |
| Banking PostgreSQL | `localhost:5434` | `transaction-db:5432` |
| Kafka | `localhost:9092` | `kafka:29092` |
| Mailpit inbox | `http://localhost:8025` | `http://mailpit:8025` |
| Mailpit SMTP | `localhost:1025` | `mailpit:1025` |

For local debugging, auth/account/transaction/ledger/notification/approval/
reconciliation services also publish ports 8081 through 8087 on loopback.
All host port bindings are limited to `127.0.0.1`.

The gateway retains the existing paths and `lb://` discovery routes. Eureka
registrations advertise container addresses reachable on the Compose network.
The transaction client's account URL is `http://account-service:8082` inside
Docker; this change does not implement additional service-to-service credentials
or transaction business authorization.

## Credentials and initialized data

Compose reads the ignored root `.env`, generated from this machine's existing
local runtime settings. `.env.example` lists the required variables with
placeholders; it contains no usable credentials. `.local/service-environment.json`
is retained for the optional Windows launch scripts but is not read by Compose.
Neither `.env` nor `.local/` is sent to Docker build contexts.

This stack uses the existing, initialized PostgreSQL and Keycloak state. The
banking database volume contains `transaction_db`, `baccount_db`, `ledger_db`, and
`notification_db`, with the `banking_local` application role and account-number
sequence. The auth database volume contains `auth_db`. Keycloak contains the
`banking-service` realm and `banking-auth-service` client. A fresh installation
must initialize these resources and supply matching credentials; copying
`.env.example` alone does not provision them. Changing a password in `.env`
does not update a role/client stored in an existing database.

Keycloak's public issuer remains `http://localhost:8080/realms/banking-service`.
Containers obtain signing keys through `http://keycloak:8080` while validating
that public issuer. `KEYCLOAK_PUBLIC_URL` controls the public base URL. The
configuration follows Keycloak's [public hostname and backchannel settings](https://www.keycloak.org/server/hostname).

## Persistence and migration

| Named volume suffix | Contents |
| --- | --- |
| `auth-db-data` | Existing auth PostgreSQL data |
| `transaction-db-data` | Existing banking PostgreSQL data |
| `keycloak-data` | Migrated Keycloak realm, users, clients, and signing keys |
| `kafka-data` | Migrated Kafka cluster metadata, topics, records, and consumer offsets |
| `mailpit-data` | Persistent test-email inbox |

Compose prefixes these names with `banking_microservices_project_`. PostgreSQL
volumes were retained; Keycloak and Kafka data were copied while their original
containers were stopped. Kafka retains its original cluster ID. Migration
backups, including PostgreSQL dumps, are in `.local/docker-migration/data/`.

The previous standalone Mailpit used a temporary database removed on shutdown;
its old test inbox was not retained. The Compose Mailpit uses a named volume
so future test messages survive container replacement.

`docker compose stop` and ordinary `docker compose down` retain these volumes.
Do not use `docker compose down -v` unless you intend to delete this local data.
Keycloak continues to use its existing development H2 database; this is a local
development stack, not a production deployment.

## Readiness and troubleshooting

Compose waits for the database, Kafka, Keycloak, and Eureka health checks before
starting dependent applications, using [health-based dependency conditions](https://docs.docker.com/compose/how-tos/startup-order/).
Each Java image also checks its local HTTP server. Gateway and Eureka health
checks require `/actuator/health` to return 200; other service checks verify that
their HTTP server responds without a 5xx status. These checks do not exercise
every banking operation.

Allow time for Eureka registration and gateway registry refresh after startup.
`Test-Gateway.ps1` checks all eight client registrations, gateway/registry health,
and rejection of anonymous account requests. Use `docker compose logs --tail 100
<service>` when a container is unhealthy. A 503 from a routed URL indicates no
available service instance; inspect the target container and its Eureka entry.

Application containers have a 640 MiB limit and Java heap ergonomics capped at
50%; Keycloak and Kafka each have 768 MiB limits. The first source build downloads
Maven dependencies, then reuses a BuildKit Maven cache for later builds.

## Migration verification

All 14 services reached healthy status, with eight application clients registered
UP using Docker network IP addresses. The pre-migration customer could still log
in and retrieve the existing account balance. A new customer registration,
login, account creation, account listing, balance lookup, notification reads,
and token refresh also passed through the Docker gateway. Customer requests to
admin/internal APIs were rejected, and missing transaction/ledger reads returned
404. Kafka delivered both registration and account-created notifications; the
registration email arrived in Mailpit and survived Mailpit container recreation.
No banking JVMs remain running directly on Windows.

The two test customers are local fixtures; their credentials are stored only in
`.local/integration-test-user.json` and `.local/docker-integration-test-user.json`.
Transaction posting/settlement and the unimplemented approval/reconciliation
business flows were not included in these gateway and deployment checks.
