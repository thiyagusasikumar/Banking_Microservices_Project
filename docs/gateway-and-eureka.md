# API gateway and Eureka

The gateway resolves explicit `lb://<service-name>` routes through Eureka and
Spring Cloud LoadBalancer. It preserves paths, query strings, request bodies,
and bearer tokens. Backend services continue enforcing their existing roles
and ownership rules. There are no automatic routes exposing every registered
service or its actuator endpoints.

## Ports and routes

| Application | Default port | Path through gateway |
| --- | --- | --- |
| Keycloak | 8080 | Access Keycloak directly |
| auth-service | 8081 | `/auth/**` |
| account-service | 8082 | `/api/v1/accounts/**` |
| transaction-service | 8083 | `/transactions/**` |
| ledger-service | 8084 | `/ledger/**` |
| notification-service | 8085 | `/notification/**` (including SSE `/notification/stream`) |
| approval-service | 8086 | `/approvals/**` (reserved; no controllers yet) |
| reconciliation-service | 8087 | `/reconciliation/**` (reserved; no controllers yet) |
| api-gateway | 8090 | Public API entry point |
| eureka-server | 8761 | Dashboard `/`, registry `/eureka/` |

The approval and reconciliation scaffolds now include an HTTP server so they
can stay running and register on distinct ports. Their reserved routes return
404 until controllers are implemented. A route with no available registered
instance returns 503.

## Start locally

Docker Compose is now the default runtime for all applications. Follow
[Docker operations](docker-compose.md) to start the complete stack with
`docker compose up -d --build --wait --wait-timeout 240`. The Windows and
foreground workflows below are alternatives and require the corresponding
application containers to be stopped first.

### Alternative: managed Windows Java processes

PowerShell 7.4+ scripts are provided in `scripts/`. Local service overrides,
process IDs, and logs are in the git-ignored `.local/` directory. The local
overrides point auth-service at port 5433 and the other database-backed services
at port 5434, avoiding another project's PostgreSQL on port 5432. Existing data
volumes and other projects are preserved.

```powershell
Set-Location D:\Banking_Microservices_Project
# Use this alternative only when application containers are stopped.
docker compose up -d auth-db transaction-db keycloak kafka mailpit

# Build and launch all nine applications in the background.
.\scripts\Start-Banking.ps1
# For already-built jars:
.\scripts\Start-Banking.ps1 -SkipBuild

# Read-only health, service registration, and authentication checks.
.\scripts\Test-Gateway.ps1

# Stop only processes tracked by these scripts; Docker containers keep running.
.\scripts\Stop-Banking.ps1
```

To rebuild a running service, stop it first because Windows locks running jars:

```powershell
.\scripts\Stop-Banking.ps1 -Services ledger-service
.\scripts\Start-Banking.ps1 -Services ledger-service
```

Application output is in `.local/logs/<service>.log`, with errors and build logs
alongside it. `Start-Banking.ps1` reads per-service environment variables from
`.local/service-environment.json`; keep that file private because it contains
local credentials. It does not reset database passwords or Keycloak clients.
The script reports an occupied port instead of stopping another application's
process. Listening ports indicate process startup; use `Test-Gateway.ps1` to
confirm registry and gateway readiness.

During setup, the missing `banking-service` realm was imported from the project's
export with new IDs and local client secrets. The missing account, ledger, and
notification databases were created in the banking PostgreSQL volume, along
with the account-number sequence. A separate local database role was added;
existing role passwords were preserved. These are local runtime changes, not
credentials committed to source control. On a fresh machine, provision these
resources and supply equivalent environment overrides before starting services.

Notification SMTP is configured locally to Mailpit on port 1025, with its inbox
at `http://localhost:8025`. Test email is captured there. Mailpit runs from the
official `axllent/mailpit:v1.27.4` image; see its [Docker documentation](https://mailpit.axllent.org/docs/install/docker/).
The integration test customer credentials and account ID are stored privately
in `.local/integration-test-user.json`. The customer and account remain available
for local testing.

### Individual foreground startup

Use JDK 21. Each module is an independent Maven project. From
`D:\Banking_Microservices_Project`, run each long-running command in its own
PowerShell terminal:

```powershell
# Terminal 1: service registry
.\eureka-server\mvnw.cmd -f .\eureka-server\pom.xml spring-boot:run

# Other terminals: start the business services you need, for example:
.\auth-service\mvnw.cmd -f .\auth-service\pom.xml spring-boot:run
.\account-service\mvnw.cmd -f .\account-service\pom.xml spring-boot:run

# Gateway terminal
.\api-gateway\mvnw.cmd -f .\api-gateway\pom.xml spring-boot:run
```

Start the business services' existing PostgreSQL, Kafka, Keycloak, and other
configured dependencies as needed. The new gateway and registry do not require
a database. Gateway startup does not require a live Keycloak connection;
validating bearer tokens requires access to Keycloak's signing keys.

Foreground Maven commands do not automatically load `.local/service-environment.json`;
use the managed scripts above for this machine's configured database credentials.

Visit `http://localhost:8761` and wait for running services to appear as `UP`.
Eureka registration and gateway registry refresh are asynchronous; allow up to
roughly 90 seconds after startup for routes to become available.

```powershell
Invoke-RestMethod http://localhost:8090/actuator/health
Invoke-RestMethod http://localhost:8761/actuator/health

# Use the same request payload as the existing auth-service login endpoint.
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8090/auth/login `
    -ContentType 'application/json' -Body $loginJson

# Supply an access token from your normal login flow.
Invoke-RestMethod http://localhost:8090/api/v1/accounts/me `
    -Headers @{ Authorization = "Bearer $accessToken" }
```

## Environment configuration

Set variables in the terminal that launches each application. Spring Boot does
not automatically read the root `team.env.example` or a `.env` file.

| Variable | Applies to | Default |
| --- | --- | --- |
| `EUREKA_SERVER_URL` | All services and gateway | `http://localhost:8761/eureka/` |
| `SERVER_PORT` | Each application's process | Port listed above |
| `EUREKA_HOSTNAME` | Eureka server | `localhost` |
| `KEYCLOAK_SERVER_URL` | Gateway | `http://localhost:8080` |
| `KEYCLOAK_REALM` | Gateway | `banking-service` |
| `GATEWAY_ALLOWED_ORIGINS` | Gateway | `http://localhost:5173` |

`GATEWAY_ALLOWED_ORIGINS` accepts a comma-separated list of exact frontend
origins. Configure frontend API calls to use `http://localhost:8090`.
Gateway Keycloak settings must match the tokens' issuer and the backend services'
Keycloak configuration. When running on different hosts or in containers, set
`EUREKA_SERVER_URL` to the reachable registry address and ensure advertised
service IP addresses are reachable from the gateway.

For standalone backend tests or development without a registry, set
`EUREKA_CLIENT_ENABLED=false`. For a second instance of a service, use a distinct
`SERVER_PORT`; the registration ID includes service name, hostname, and port.

## Authentication and scope

Only POST `/auth/login`, `/auth/public/register`, and `/auth/refresh`, plus GET
`/actuator/health` and `/actuator/info`, are anonymous. Other requests require a
valid Keycloak JWT; the gateway checks its signature, issuer, and expiry and
forwards the original bearer token to the backend. Admin authorization remains
in auth-service, where it already uses database-backed role conversion.

`/api/v1/accounts/operations` and its subpaths are blocked at the gateway because
they are internal transaction-service APIs. The existing transaction-to-account
HTTP client continues to use its configured `account-service.base-url`; changing
that client or its service credentials is separate from gateway routing.

The gateway does not retry banking writes automatically. It sets a connection
timeout but leaves the response timeout unset to support notification SSE.
Only health and info actuator endpoints are exposed. Eureka is configured as a
standalone registry with self-registration and peer fetching disabled; its
default self-preservation behavior remains enabled.

This is a local development setup. Keep Eureka and backend ports on the private
network when deploying, with external traffic entering through the gateway.
The gateway's JWT check does not add missing business authorization rules to
services that do not yet implement them.

## Build and test

```powershell
.\api-gateway\mvnw.cmd -f .\api-gateway\pom.xml verify
.\eureka-server\mvnw.cmd -f .\eureka-server\pom.xml verify
```

Gateway tests run on random ports with local upstreams and discovery instances,
and a mocked JWT decoder. They verify forwarding, public auth endpoints,
protected requests, internal-route blocking, CORS, unknown routes, and missing
instances. Eureka tests exercise its actual HTTP registration, heartbeat,
lookup, deregistration, dashboard, and health endpoints. Neither suite requires
external infrastructure.

Live integration was also verified against the actual local services: all eight
clients registered `UP`; registration, login, customer profile, account creation,
account listing, balance lookup, notification reads, and token refresh succeeded
through the gateway. Anonymous requests and customer access to admin/internal
APIs were rejected. Missing transaction and ledger records return 404. The ledger
not-found exception mapping was corrected and covered by a controller regression
test. Transaction posting/settlement and the unimplemented approval/reconciliation
business flows are outside these gateway checks.

The existing Boot 3.5 modules use Spring Cloud 2025.0.3; the existing Boot 4.1
modules use Spring Cloud 2025.1.4. Their Boot versions are preserved, with Cloud
versions selected using the [official compatibility table](https://spring.io/projects/spring-cloud/).
See also the official [Eureka configuration](https://docs.spring.io/spring-cloud-netflix/reference/spring-cloud-netflix.html)
and [Gateway starter documentation](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/starter.html).

