# Vaultx — Real-Time Bidding Platform

## Project Description (for Resume / Portfolio)

Vaultx is a production-grade, event-driven **Real-Time Bidding Platform** built with a **Microservices Architecture** on **Java 21** and **Spring Boot 3**. Designed to handle hundreds of thousands of concurrent users participating in thousands of simultaneous auctions, Vaultx demonstrates distributed systems engineering patterns used at FAANG-scale companies.

### Key Technical Achievements

- **5 independent microservices** (API Gateway, User Service, Bidding Service, Transaction Service, Notification Service) with **database-per-service** isolation, each owning its own PostgreSQL instance with zero shared databases or cross-service foreign keys.

- **Real-time bidding engine** supporting **500+ bids/second** using **optimistic locking** (version-based concurrency control), **idempotency keys** for duplicate prevention, and **soft-close auction extensions** to prevent sniping — all with strong consistency guarantees.

- **Inter-service communication** via **gRPC** (Protocol Buffers) for synchronous calls — 7x faster than REST/JSON — and **Apache Kafka** for asynchronous event-driven workflows with **transactional outbox pattern**, **dead letter queues**, and **exponential backoff retries**.

- **Security**: JWT (RS256 signed) with refresh token rotation, BCrypt password hashing, Spring Security role-based authorization (USER/SELLER/ADMIN), **Redis-based rate limiting** (sliding window), and comprehensive input validation.

- **Observability**: Distributed tracing (Zipkin), Prometheus metrics, Grafana dashboards, structured JSON logging with correlation IDs, and Spring Boot Actuator health endpoints.

- **Resilience**: Circuit breakers (Resilience4j), gRPC deadlines/timeouts/retries, Kafka DLQs, graceful shutdown hooks, and compensating transactions (Saga pattern) for payment failures.

- **Containerized** with Docker Compose — `docker compose up` launches the entire platform including Kafka, Redis, PostgreSQL (×3), Prometheus, Grafana, and Zipkin.

- **Tested** with JUnit 5, Mockito, and **Testcontainers** for integration tests against real PostgreSQL, Kafka, and Redis instances.

### Architecture Style

Event-Driven Microservices | Clean Architecture | DDD principles | SOLID

### Tech Stack

`Java 21` · `Spring Boot 3` · `Spring Cloud Gateway` · `Spring Security` · `gRPC` · `Apache Kafka` · `PostgreSQL` · `Redis` · `Docker` · `Prometheus` · `Grafana` · `Zipkin` · `JUnit 5` · `Testcontainers` · `Maven`

### What This Demonstrates

| Skill | Evidence in Project |
|---|---|
| **Distributed Systems** | Microservices, event-driven architecture, transactional outbox, saga pattern |
| **Concurrency** | Optimistic locking, virtual threads, race condition prevention, idempotency |
| **System Design** | Capacity planning, database per service, CQRS-adjacent patterns, soft-close auctions |
| **Production Engineering** | Observability (metrics/tracing/logging), resiliency (circuit breakers/DLQs), graceful shutdown |
| **Security** | JWT, RBAC, rate limiting, BCrypt, input validation, secure headers |
| **DevOps** | Docker Compose, CI/CD pipeline (GitHub Actions), multi-stage Docker builds |

### Repository Structure

```
vaultx/
├── api-gateway/           # Spring Cloud Gateway (JWT validation, rate limiting, routing)
├── user-service/          # Auth, profiles, wallets, KYC
├── bidding-service/       # Auctions, bids, auto-bidding, state machine
├── transaction-service/   # Payments, escrow, refunds
├── notification-service/  # Email/SMS/push (Kafka consumer)
├── docker-compose.yml     # One-command startup
└── ARCHITECTURE.md        # Comprehensive design document
```
