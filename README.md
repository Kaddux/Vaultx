# Vaultx — Real-Time Bidding Platform

A production-grade, event-driven **Real-Time Bidding Platform** built with Java, Spring Boot 3, Apache Kafka, gRPC, and PostgreSQL. Buyers and sellers participate in live auctions with real-time bidding, wallet-based escrow, KYC verification, and automated auction lifecycle management.

## Architecture

```
┌────────────────────────────────────────────────────────────┐
│                   React SPA (Port 3000)                    │
└──────────────────────────┬─────────────────────────────────┘
                           │ REST
                           ▼
┌────────────────────────────────────────────────────────────┐
│                 API Gateway (Port 8080)                    │
│          Spring Cloud Gateway · JWT · Rate Limit · CORS    │
└───┬──────────────┬──────────────┬──────────────┬───────────┘
    │              │              │              │
    ▼              ▼              ▼              ▼
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│  User    │  │ Bidding  │  │Transact. │  │Notific.  │
│ Service  │  │ Service  │  │ Service  │  │ Service  │
│  :8000   │  │  :8001   │  │  :8002   │  │  :8003   │
│ gRPC:9000│  │ gRPC:9001│  │ gRPC:9002│  │  Kafka   │
└────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘
     │             │             │             │
  PostgreSQL     PostgreSQL    PostgreSQL    PostgreSQL
    :5000          :5001         :5002         :5003
     │             │             │             │
     └─────────────┴─────┬───────┴─────────────┘
                         ▼
              Apache Kafka (KRaft) · Redis · Zipkin
                         │
                    Prometheus · Grafana
```

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.4.4 |
| Gateway | Spring Cloud Gateway (WebFlux/Netty) |
| Security | Spring Security, JWT (RS256), BCrypt |
| RPC | gRPC + Protocol Buffers |
| Messaging | Apache Kafka 7.6 (KRaft) + Confluent Schema Registry |
| Database | PostgreSQL 15 (one per service) |
| Cache | Redis 7 (rate limiting) |
| Observability | Micrometer, Prometheus, Grafana, Zipkin, structured JSON logs |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS |
| Build | Maven (multi-module) |
| CI/CD | GitHub Actions |
| Deployment | Docker Compose |

## Microservices

| Service | Port | HTTP | gRPC | Responsibility |
|---|---|---|---|---|
| `api-gateway` | 8080 | ✅ | — | Routing, JWT validation, rate limiting, correlation IDs, security headers |
| `user-service` | 8000 | ✅ | 9000 | Auth, users, wallets, KYC, refresh tokens |
| `bidding-service` | 8001 | ✅ | 9001 | Auctions, bids, scheduler, outbox → Kafka |
| `transaction-service` | 8002 | ✅ | 9002 | Payments, escrow, refunds (Saga) |
| `notification-service` | 8003 | ✅ | — | Kafka consumer, email/SMS/push simulation, preferences |

## Key Features

- **Microservices** with database-per-service isolation (zero shared DBs)
- **Event-driven** with the **transactional outbox pattern** → Kafka topics per event type
- **gRPC** for low-latency sync inter-service calls (wallet checks, profiles)
- **JWT RS256 auth** with refresh-token rotation; HMAC-free asymmetric signing
- **Optimistic + pessimistic locking** for concurrent bid safety
- **Idempotency keys** (UNIQUE constraints) to prevent duplicate bids/payments
- **Escrow saga**: bid → win → hold → release/refund with compensation
- **Rate limiting** via Redis (per-user sliding window on the bid endpoint)
- **Observability**: Prometheus metrics, Grafana dashboards, Zipkin tracing, correlation-ID JSON logs
- **Soft-close** auction extension, auto-bid fields, KYC verification flow
- **CI pipeline** (GitHub Actions) + **Testcontainers** integration tests

## Quick Start

### Prerequisites
- Docker + Docker Compose
- JDK 17 (for local dev)
- Maven (or use the included `mvnw` wrapper)

### Run the full stack (Docker)

```bash
docker compose up --build
```

All 15 containers start: 5 services + gateway, 4 PostgreSQL DBs, Kafka, Schema Registry, Redis, Zipkin, Prometheus, Grafana.

| Service | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin) |
| Zipkin | http://localhost:9411 |

### Run services locally (infra in Docker)

```bash
docker compose up kafka schema-registry user-service-db bid-service-db tx-service-db notification-db redis
# then run each service via IDE or:
./mvnw -pl user-service spring-boot:run
./mvnw -pl bidding-service spring-boot:run
./mvnw -pl transaction-service spring-boot:run
./mvnw -pl notification-service spring-boot:run
./mvnw -pl api-gateway spring-boot:run
```

### Run the frontend

```bash
cd frontend
npm install
npm run dev     # http://localhost:3000
```

## Demo User

A demo user is auto-seeded on user-service startup:

```
Email:    demo@vaultx.io
Password: Demo1234!
Role:     SELLER (KYC VERIFIED)
Wallet:   $10,000.00
```

> Register a second user to test bidding as a buyer (sellers can't bid on their own auctions).

## API Overview (21 endpoints)

All requests go through the gateway at `http://localhost:8080`. Protected routes require `Authorization: Bearer <accessToken>`.

| Service | Method | Path | Auth |
|---|---|---|---|
| User | POST | `/api/auth/register` | — |
| User | POST | `/api/auth/login` | — |
| User | POST | `/api/auth/refresh` | — |
| User | GET | `/api/users/me` | 🔒 |
| User | PATCH | `/api/users/me` | 🔒 |
| User | DELETE | `/api/users/me` | 🔒 |
| User | GET | `/api/wallet` | 🔒 |
| User | POST | `/api/wallet/deposit` | 🔒 |
| Bidding | POST | `/api/auctions` | 🔒 |
| Bidding | GET | `/api/auctions` | — |
| Bidding | GET | `/api/auctions/{id}` | — |
| Bidding | POST | `/api/auctions/{id}/bids` | 🔒 |
| Bidding | GET | `/api/auctions/{id}/bids` | — |
| Bidding | GET | `/api/auctions/{id}/bids/mine` | 🔒 |
| Transaction | POST | `/api/payments/release` | 🔒 |
| Transaction | POST | `/api/payments/refund` | 🔒 |
| Transaction | GET | `/api/payments/{auctionId}` | 🔒 |
| Notification | POST | `/api/notifications/request` | — |
| Notification | GET | `/api/notifications` | 🔒 |
| Notification | GET | `/api/notifications/unread-count` | 🔒 |
| Notification | PUT | `/api/notifications/preferences/{eventType}` | 🔒 |

**Example login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@vaultx.io","password":"Demo1234!"}'
```

## Project Structure

```
Vaultx/
├── pom.xml                     # Parent Maven (multi-module)
├── docker-compose.yml          # Full stack orchestration
├── architecture.md             # Systems architecture blueprint
├── workflows.md                # End-to-end workflows
├── WORKFLOW.md                 # Code & workflow explanation
├── INTERVIEW_QA.md             # Backend interview Q&A
├── prometheus.yml              # Metrics scrape config
├── grafana/                    # Dashboards + provisioning
├── .github/workflows/ci.yml    # CI pipeline
├── api-gateway/                # Spring Cloud Gateway
├── user-service/               # Auth, users, wallets, KYC
├── bidding-service/            # Auctions, bids, scheduler
├── transaction-service/        # Payments, escrow, refunds
├── notification-service/       # Kafka consumer, channels
└── frontend/                   # React + Vite SPA
```

## Event Flow (Bid → Payment)

```
Bid placed → Bidding Service validates (gRPC wallet) → saves bid + outbox event
→ OutboxPoller → Kafka `bid.placed`
→ AuctionScheduler → auction SOLD → outbox → Kafka `auction.won`
→ Transaction Service consumes → debits buyer wallet (gRPC) → escrow HELD
→ POST /api/payments/release → credits seller → escrow RELEASED
→ Notification Service delivers email/SMS/push (simulated)
```

## Testing

```bash
# Unit tests per service
./mvnw -f user-service/pom.xml test
./mvnw -f bidding-service/pom.xml test
./mvnw -f transaction-service/pom.xml test

# Integration tests (requires Docker)
./mvnw -f transaction-service/pom.xml verify -P integration-tests
```

## Security Notes

- JWT is signed with **RS256** using an RSA keypair. Dev keys are committed for convenience; **override in production** via `JWT_PRIVATE_KEY_PATH` / `JWT_PUBLIC_KEY_PATH` env vars (point to a secrets manager).
- Passwords hashed with BCrypt.
- Refresh tokens stored in DB with rotation + revocation.
- Rate limiting, security headers, and CORS handled at the gateway.

## License

Private / educational portfolio project.
