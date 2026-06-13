# CircleCI + Spring Boot + Testcontainers example

This example shows how to run Spring Boot integration tests with Testcontainers in CircleCI.

## What is included

- Spring Boot 3.5
- Java 17
- Spring Data JPA
- MySQL Testcontainer
- Maven Failsafe integration test setup
- CircleCI `machine` executor with Docker available

## Run locally

```bash
mvn clean verify
```

The integration test starts a real MySQL 8.4 container and verifies a JPA repository.

## CircleCI

The file `.circleci/config.yml` runs:

```bash
mvn clean verify
```

The `machine` executor is used because Testcontainers needs access to Docker.
