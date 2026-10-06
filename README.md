# Banking Microservices Project

This Java 21 / Spring Boot 3.5.16 practice workspace follows the supplied banking microservices outline.

## Projects

| Project | State |
| --- | --- |
| auth-service | Initial Spring Boot Maven project only |
| account-service | Initial Spring Boot Maven project only |
| transaction-service | Implemented; see its README |
| approval-service | Initial Spring Boot Maven project only |
| ledger-service | Initial Spring Boot Maven project only |
| notification-service | Initial Spring Boot Maven project only |
| reconciliation-service | Initial Spring Boot Maven project only |
| api-gateway | Initial Spring Boot Maven project only |

Each folder is independent, with its own Maven wrapper, `pom.xml`, application class, properties file, and generated context test. The other seven services have no business implementation yet.

## Local infrastructure

Run `docker compose up -d` here for Transaction Service's PostgreSQL database and Kafka broker. Then run `mvn spring-boot:run` inside `transaction-service`. The Account Service operation contract must be implemented before money movements can complete; the current Account Service folder is intentionally only a starter project.

This is a learning project. Use test data only.
