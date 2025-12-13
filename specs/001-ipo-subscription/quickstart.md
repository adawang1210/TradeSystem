# Quickstart: TradeSystem IPO Simulation

## Prerequisites
- Java 17+
- Maven Wrapper scripts included in repo (`./mvnw`)

## Run the Application
```bash
./mvnw spring-boot:run
```
This launches the Spring Boot application with the in-memory data repository. Access the UI via the configured localhost port.

## Run Tests
```bash
./mvnw test
```
Executes the full JUnit suite, including concurrency simulation and service-layer tests.
