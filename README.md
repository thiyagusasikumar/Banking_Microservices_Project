# Banking Microservices Project

## Run with Docker Compose

The nine Spring applications and their infrastructure now run in Compose. From
this directory, with Docker Desktop running and the local `.env` configured:

```powershell
docker compose up -d --build --wait --wait-timeout 240
docker compose ps
.\scripts\Test-Gateway.ps1
```

Gateway: `http://localhost:8090`; Eureka: `http://localhost:8761`;
Keycloak: `http://localhost:8080`; captured test mail: `http://localhost:8025`.
Use `docker compose stop` to stop the stack while retaining containers and data.
See [Docker operations](docs/docker-compose.md) for configuration, logs, rebuilds,
and data persistence. The existing `.env` contains local credentials and is ignored by Git.



## Projects

| Project | State |
| --- | --- |
| auth-service | Existing authentication API; Docker and Eureka configured |
| account-service | Existing account API; Docker and Eureka configured |
| transaction-service | Existing transaction API; Docker and Eureka configured |
| approval-service | Scaffold; Docker and Eureka configured |
| ledger-service | Existing ledger API; Docker and Eureka configured |
| notification-service | Existing notification API; Docker and Eureka configured |
| reconciliation-service | Scaffold; Docker and Eureka configured |
| api-gateway | Eureka-backed routing, Keycloak JWT validation, and CORS on port 8090 |
| eureka-server | Standalone service registry and dashboard on port 8761 |

## Gateway and service discovery

See [Gateway and Eureka setup](docs/gateway-and-eureka.md) for routes,
configuration, and verification. Compose manages startup dependencies.
The existing service URLs are available through
`http://localhost:8090`; Keycloak continues to use port 8080.
