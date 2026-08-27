# Vaultx - Workflow & Code Explanation

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack](#2-tech-stack)
3. [Architecture](#3-architecture)
   - 3a. [How the System Works (End-to-End)](#3a-how-the-system-works-end-to-end-overview)
   - 3b. [Communication Between Services](#3b-communication-between-services)
   - 3c. [Tool Usage Per Service](#3c-tool-usage-per-service)
   - 3d. [Design Patterns Used](#3d-design-patterns-used)
4. [Service-by-Service Breakdown](#4-service-by-service-breakdown)
5. [Core Workflows](#5-core-workflows)
6. [Data Flow Diagrams](#6-data-flow-diagrams)
7. [Database Schema](#7-database-schema)
8. [Infrastructure & Deployment](#8-infrastructure--deployment)
9. [Security Architecture](#9-security-architecture)
10. [Current Status & Gaps](#10-current-status--gaps)

---

## 1. Project Overview

Vaultx is a **Real-Time Bidding Platform** built as a microservices system. Users can register, complete KYC verification, fund wallets, create auctions, and place bids in real-time. The system uses an event-driven architecture with gRPC for synchronous inter-service communication and Kafka for asynchronous event messaging.

---

## 2. Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 (Docker runs JDK 21) |
| Framework | Spring Boot 3.4.4 |
| RPC | gRPC 1.58/1.69 + Protocol Buffers |
| Messaging | Apache Kafka 7.6.0 (KRaft mode, Confluent) |
| Database | PostgreSQL 15 (Alpine) - one per service |
| Security | Spring Security, JWT (JJWT 0.12.6), BCrypt |
| ORM | Spring Data JPA / Hibernate |
| Build | Maven (multi-module parent POM) |
| Containerization | Docker + Docker Compose |
| Frontend | React 18 + TypeScript + Vite + Tailwind CSS |

---

## 3. Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   REACT SPA (Port 3000)                 │
│            Mock data, no API wiring yet                  │
└──────────────────────┬──────────────────────────────────┘
                       │ REST (planned via API Gateway)
                       ▼
┌──────────────────────────┐    gRPC     ┌──────────────────────────┐
│      USER SERVICE        │◄───────────►│    BIDDING SERVICE       │
│   HTTP: 8000             │             │    HTTP: 8001            │
│   gRPC: 9000             │             │    gRPC: 9001            │
│   DB: PostgreSQL (5000)  │             │    DB: PostgreSQL (5001) │
└──────────────────────────┘             └───────────┬──────────────┘
                                                     │ Kafka Events
                                                     ▼
                                         ┌──────────────────────────┐
                                         │  TRANSACTION SERVICE     │
                                         │   HTTP: 8002             │
                                         │   gRPC: 9002             │
                                         │   DB: PostgreSQL (5002)  │
                                         └──────────────────────────┘
```

---

## 3a. How the System Works (End-to-End Overview)

Vaultx operates as a **choreographed saga** — no central orchestrator exists. Instead, each service reacts to events produced by others, forming a distributed state machine.

### High-Level Flow

```
  USER                           SYSTEM
   │                                │
   ├─ Register ──────────────────►  │ User Service: creates user + wallet
   │                                │
   ├─ Login ─────────────────────►  │ User Service: validates, returns JWT
   │                                │
   ├─ Deposit Funds ─────────────►  │ User Service: credits wallet balance
   │                                │
   ├─ Create Auction ────────────►  │ Bidding Service: saves PENDING auction
   │                                │                          → outbox AUCTION_CREATED
   │                                │                          → Kafka: auction.created
   │                                │
   │  [Time passes]                │ ── AuctionScheduler ──► Bidding Service:
   │                                │     PENDING → ACTIVE   → outbox AUCTION_STARTED
   │                                │                          → Kafka: auction.started
   │                                │
   ├─ Place Bid ──────────────────►  │ Bidding Service:                      │
   │                                │   ├─ gRPC → User Service: get wallet  │
   │                                │   ├─ Validate funds & auction state   │
   │                                │   ├─ Save bid (pessimistic lock)      │
   │                                │   ├─ Update auction.currentBid         │
   │                                │   └─ outbox BID_PLACED                │
   │                                │                          → Kafka: bid.placed
   │                                │
   │  [Time passes / Auction ends]  │ ── AuctionScheduler ──► Bidding Service:
   │                                │     ACTIVE → SOLD/UNSOLD
   │                                │     → outbox AUCTION_ENDED
   │                                │     → if SOLD: outbox AUCTION_WON
   │                                │                          → Kafka: auction.ended
   │                                │                          → Kafka: auction.won
   │                                │
   │                                │ ── Transaction Service (planned): ──►
   │                                │     Consumes auction.won
   │                                │     → gRPC UpdateWallet: hold escrow
   │                                │     → On confirm: release to seller
   │                                │     → On dispute: refund buyer
```

### Event Flow Catalog

Every domain action triggers a chain of events across the system:

| Domain Event | Producer | Kafka Topic | Consumer(s) | Planned Consumer |
|---|---|---|---|---|
| User registered | User Service | (stays local) | - | Notification Service |
| Auction created | Bidding Service | `auction.created` | Transaction Service | Notification, Analytics |
| Auction started | Bidding Service | `auction.started` | Transaction Service | Notification, Analytics |
| Bid placed | Bidding Service | `bid.placed` | Transaction Service | Analytics, Real-time feed |
| Auction ended | Bidding Service | `auction.ended` | Transaction Service | Notification, Analytics |
| Auction won | Bidding Service | `auction.won` | Transaction Service | Notification, Analytics |
| Auction lost | Bidding Service | `auction.lost` | Transaction Service | Notification, Analytics |
| Wallet credited | User Service | (via gRPC response) | Bidding Service | - |
| Payment processed | Transaction Service | (not yet) | - | Notification, Analytics |

### Data Ownership Per Service

```
┌──────────────────────────────────────────────────────────┐
│                   SERVICE BOUNDARIES                     │
├────────────────┬──────────────────┬─────────────────────┤
│  USER SERVICE  │  BIDDING SERVICE │ TRANSACTION SERVICE │
│  Owns:         │  Owns:           │  Owns:              │
│  • users       │  • auctions      │  • transactions     │
│  • wallets     │  • bids          │  • escrows          │
│  • refresh_    │  • outbox_events │  • outbox_events    │
│    tokens      │                  │                     │
│                │                  │                     │
│  Exposes via:  │  Exposes via:    │  Exposes via:       │
│  • gRPC:       │  • gRPC:         │  • gRPC: (planned)  │
│    user/       │    auction       │    transaction      │
│    wallet      │    details       │    history          │
│  • REST:       │  • REST:         │                     │
│    auth/       │    auctions/     │                     │
│    users/      │    bids          │                     │
│    wallet/     │                  │                     │
└────────────────┴──────────────────┴─────────────────────┘
```

### State Transitions

**Auction States:**
```
    ┌──────────┐
    │ PENDING  │  ← Created by seller, not yet started
    └────┬─────┘
         │ [startTime <= now] — AuctionScheduler
         ▼
    ┌──────────┐
    │ ACTIVE   │  ← Accepting bids
    └────┬─────┘
         │ [endTime <= now] — AuctionScheduler
         ▼
    ┌──────────┐     ┌──────────┐
    │  SOLD    │     │ UNSOLD   │  ← No bids or reserve not met
    └──────────┘     └──────────┘
```

**Bid States:**
```
    ┌──────────┐
    │ WINNING  │  ← Highest bid on active auction
    └────┬─────┘
         │ [new higher bid placed]
         ▼
    ┌──────────┐     ┌──────────┐
    │ OUTBID   │     │   WON    │  ← Auction ended, was the winner
    └──────────┘     └──────────┘

    ┌──────────┐
    │   LOST   │  ← Auction ended, was not the winner
    └──────────┘
```

---

## 3b. Communication Between Services

### Communication Matrix

Every pair of services communicates over specific protocols for specific purposes:

| Source Service | Target Service | Protocol | Direction | Purpose | Data Format | Sync/Async | Frequency | Reliability |
|---|---|---|---|---|---|---|---|---|
| **Bidding** | **User** | gRPC | Outbound | Get wallet balance before bid placement | Protobuf (binary) | Synchronous | Per bid placed | High — bid fails if unavailable |
| **Bidding** | **User** | gRPC | Outbound | Get user profile (KYC status, email) | Protobuf (binary) | Synchronous | Per auction create/view | High |
| **Transaction** | **User** | gRPC | Outbound | Credit/debit wallet (escrow hold/release) | Protobuf (binary) | Synchronous | Per auction close Planned | Critical — must succeed for payment |
| **Transaction** | **User** | gRPC | Outbound | Get wallet balance for verification | Protobuf (binary) | Synchronous | Per payment Planned | High |
| **Transaction** | **Bidding** | gRPC | Outbound | Get auction details for settlement | Protobuf (binary) | Synchronous | Per auction close Planned | Medium |
| **Bidding** | **Kafka** | Kafka Producer | Outbound | Publish outbox events to topics | JSON (string payload) | Asynchronous | Every 5 seconds (batch) | At-least-once |
| **Transaction** | **Kafka** | Kafka Producer | Outbound | Publish payment events (planned) | JSON / Avro | Asynchronous | Per payment Planned | At-least-once |
| **Transaction** | **Kafka** | Kafka Consumer | Inbound | Consume auction lifecycle events | JSON | Asynchronous | Event-driven | At-least-once |

### gRPC Communication Details

```
┌─────────────────────┐         gRPC (Port 9000)         ┌─────────────────────┐
│                     │ ◄─────────────────────────────── │                     │
│   USER SERVICE      │    GetUserProfile(userId)        │   BIDDING SERVICE   │
│   (gRPC Server)     │    GetWalletBalance(userId)      │   (gRPC Client)     │
│                     │                                   │                     │
│   Proto:            │    UpdateWallet(userId,amount,   │   Proto Copy:       │
│   user_service.proto│      type,idempotencyKey)        │   user_service.proto│
└─────────────────────┘                                   └─────────────────────┘
        ▲                                                         │
        │                                                         │
        │        gRPC (Port 9000 & 9001)                         │
        │                                                         │
        │    GetUserProfile(userId)                               │
        │    GetWalletBalance(userId)                             │
        │    UpdateWallet(...)                                    │
        │                                                         ▼
┌─────────────────────┐                               ┌─────────────────────┐
│                     │                               │                     │
│   USER SERVICE      │                               │   BIDDING SERVICE   │
│                     │                               │   (gRPC Server)     │
│                     │                               │                     │
│                     │                               │   Proto:            │
│                     │   GetAuctionDetails(auctionId) │   bidding_service   │
│                     │ ◄──────────────────────────── │   .proto            │
│      TRANSACTION SERVICE (gRPC Client) ───────────► └─────────────────────┘
```

### Kafka Communication Details

```
┌──────────────────┐                       ┌──────────────────┐
│   BIDDING        │                       │   TRANSACTION    │
│   SERVICE        │                       │   SERVICE        │
│                  │                       │                  │
│  Topics Produced:│                       │ Topics Consumed: │
│  ┌────────────┐  │   ┌──────────┐       │ ┌──────────────┐ │
│  │bid.placed  │──┼──►│  KAFKA   │───────┼─►│bid.placed    │ │
│  │auction.    │  │   │          │       │ │auction.created│ │
│  │ created    │──┼──►│  Broker  │───────┼─►│auction.started│ │
│  │auction.    │  │   │ (Port    │       │ │auction.ended │ │
│  │ started    │──┼──►│  9092)   │───────┼─►│auction.won   │ │
│  │auction.    │  │   │ ┌──────┐ │       │ │auction.lost  │ │
│  │ ended      │──┼──►│ │      │ │───────┼─►│              │ │
│  │auction.won │  │   │ │Topics│ │       │ │Topics Produced:│
│  │auction.lost│──┼──►│ │  +   │ │       │ │┌─────────────┐│ │
│  └────────────┘  │   │ │Retry │ │       │ ││payment.     ││ │
│                  │   │ │+ DLQ │ │       │ ││ completed   ││ │
│                  │   │ └──────┘ │       │ ││payment.     ││ │
│                  │   └──────────┘       │ ││ refunded    ││ │
│                  │                      │ └─────────────┘│ │
└──────────────────┘                      └──────────────────┘
```

### How Services Find Each Other

**In Docker:**
- Services use Docker DNS resolution (service name → container IP)
- `SPRING_DATASOURCE_URL=jdbc:postgresql://user-service-db:5432/db`
- `GRPC_USER_SERVICE_HOST=user-service`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092`

**In Local Development:**
- Services use `localhost` with explicit ports
- `grpc.client.user-service.address=localhost:9000`
- `spring.kafka.bootstrap-servers=localhost:9092`
- `spring.datasource.url=jdbc:postgresql://localhost:5000/db`

### Failure Handling in Communication

| Failure Scenario | Current Behavior | Improvement Needed |
|---|---|---|
| User Service gRPC down | Bid fails with RuntimeException | Circuit breaker (Resilience4j) |
| Kafka broker down | Outbox poller logs error, events stay unpublished | Auto-retry on next poll cycle |
| Bid service DB connection lost | Auction operations fail until DB recovers | Connection pool retry |
| gRPC slow response (>500ms) | Request thread blocked | Timeout configuration, async fallback |

---

## 3c. Tool Usage Per Service

### User Service — Tool Map

| Tool / Library | Usage | Why This Tool | Code Location |
|---|---|---|---|
| **Spring Data JPA** | ORM for `Users`, `Wallet`, `RefreshToken` entities | Declarative repositories, automatic CRUD, `@Transactional` | `model/*.java`, `repository/*.java` |
| **Spring Security** | Authentication filter chain, role-based access | Industry-standard for Spring apps, integrates with method security | `security/*.java` |
| **JJWT (io.jsonwebtoken)** | JWT creation, signing, parsing, validation | Lightweight, actively maintained, HMAC support | `JwtTokenProvider.java` |
| **BCrypt (via Spring Security)** | Password hashing | Adaptive hash function, built-in salt, configurable cost factor | `SecurityConfig.java:passwordEncoder()` |
| **gRPC (Server)** | Inter-service RPC for user profile, wallet queries | Contract-first, type-safe, high-performance binary protocol | `UserServiceGrpc.java`, `user_service.proto` |
| **PostgreSQL** | Primary data store for user/auth/wallet data | ACID compliance, mature, strong community | `application.properties` |
| **Jakarta Validation** | Request DTO validation (@NotBlank, @Email, @Min, etc.) | Declarative validation, standardized | `DTO/*RequestDTO.java` |
| **Lombok** | `@RequiredArgsConstructor`, `@Getter`, `@Setter` | Reduces boilerplate, compile-time code generation | Throughout all classes |
| **Spring Boot Test** | Unit and integration tests with H2 | MockMvc for controllers, InMemory DB for repos | `src/test/*` |
| **H2 Database** | In-memory database for tests | Fast, no external dependencies, SQL-compatible | Test `application.properties` |

### Bidding Service — Tool Map

| Tool / Library | Usage | Why This Tool | Code Location |
|---|---|---|---|
| **Spring Data JPA** | ORM for `Auction`, `Bid`, `OutboxEvent` | Optimistic locking (`@Version`), pessimistic lock (`@Lock`), custom queries | `model/*.java`, `repository/*.java` |
| **Spring Kafka** | Kafka producer for outbox events, topic admin config | Idempotent producers (`enable.idempotence=true`), async send with callbacks | `KafkaConfig.java`, `OutboxPoller.java` |
| **gRPC (Client)** | Synchronous calls to User Service for wallet balance | Low-latency, type-safe, needed for bid validation in request path | `UserGrpcClient.java`, `user_service.proto` |
| **gRPC (Server)** | Expose auction details to Transaction Service | Same benefits as above — contract-first, binary protocol | `BiddingGrpcService.java`, `bidding_service.proto` |
| **Jackson** | Serialize/deserialize outbox event payloads to/from JSON | Flexible, tree model support for unstructured events (AUCTION_STARTED uses Map) | `OutboxPoller.java`, `BidService.java`, `AuctionScheduler.java` |
| **`@Scheduled`** | Cron-less periodic execution for scheduler and outbox poller | No external scheduler needed, Spring-managed thread pool | `AuctionScheduler.java`, `OutboxPoller.java` |
| **PostgreSQL** | Primary data store for auctions, bids, outbox | Same as User Service — consistent across microservices | `application.properties` |
| **CORS Config** | Allow React frontend to call REST APIs | SPA runs on different port (3000/5173 vs 8001) | `CorsConfig.java` |
| **Jakarta Validation** | Request DTO validation | Consistent validation across all service boundaries | `DTO/*Request.java` |
| **Spring Boot Test** | Service/controller/repository tests with H2 | Verify bid placement logic, auction scheduling, repository queries | `src/test/*` |

### Transaction Service — Tool Map

| Tool / Library | Usage | Why This Tool | Code Location |
|---|---|---|---|
| **Spring Kafka** | Kafka producer config for payment events | Same idempotent producer setup as bidding service | `KafkaConfig.java` |
| **Kafka Admin** | Declare same 12 topics (main + retry + DLQ) | Topic existence guarantees, prevents auto-creation mismatches | `KafkaTopicConfig.java` |
| **gRPC (Client)** | Planned: call User Service for wallet update | Bidirectional service dependency | `src/main/proto/*.proto` |
| **gRPC (Client)** | Planned: call Bidding Service for auction details | Verify auction state before processing payment | `src/main/proto/*.proto` |
| **Apache Avro** | Schema-based serialization for Kafka events (planned) | Schema evolution, compatibility checking with Schema Registry | `pom.xml` dependency |
| **Jackson** | Deserialize incoming JSON events | Parse Kafka messages from bidding service | `dto/*.java` |
| **Spring Data JPA** | ORM for `OutboxEvent` + planned transaction entities | Consistent with other services | `model/`, `repository/` |

### Kafka — Tool Role

| Kafka Feature | Used For | Configuration |
|---|---|---|
| **Topics** | Event channels: `bid.placed`, `auction.*` | 6 partitions for main, 3 for retry, 1 for DLQ |
| **Producer Idempotence** | Exactly-once semantics per partition | `enable.idempotence=true`, `acks=all` |
| **KRaft Mode** | No Zookeeper dependency | `KAFKA_PROCESS_ROLES: broker,controller` |
| **Retry Backoff** | Transient failure recovery | `retries=3`, `retry.backoff.ms=1000` |
| **Schema Registry** | Event schema management (avro only) | Port 8081, connects to Kafka |
| **Dead Letter Queue** | Poison message handling | `*.dlq` topics, `DltHandler.java` |

### Docker — Tool Role

| Docker Feature | Used For | Details |
|---|---|---|
| **Containers** | Service isolation | Each service + DB in its own container |
| **Multi-stage builds** | Smaller production images | Maven build stage → JRE Alpine runtime |
| **Volume mounts** | Data persistence across restarts | `user-db-data`, `bid-db-data` |
| **Health checks** | Dependency ordering | `pg_isready`, `kafka-topics --list`, `nc -z` |
| **Environment variables** | Runtime configuration | `SPRING_DATASOURCE_*`, `SPRING_KAFKA_*` |

---

## Design Patterns Used

| Pattern | Where | Why |
|---|---|---|
| **Database-per-Service** | Each microservice has its own PostgreSQL | Data isolation, independent scaling |
| **Transactional Outbox** | `OutboxEvent` entity in bidding-service | Reliable event publishing without 2PC |
| **CQRS (Lite)** | Separate read models via gRPC queries | Services query each other for read-only data |
| **Idempotency** | `idempotencyKey` on Bid and WalletDeposit | Prevent duplicate operations |
| **Optimistic Locking** | `@Version` on Auction and Wallet entities | Prevent lost updates on concurrent writes |
| **Saga (Choreography)** | Kafka events chain auction lifecycle | Distributed transaction coordination |

---

## 4. Service-by-Service Breakdown

### 4.1 User Service (Port 8000/9000)

**Package:** `com.pm.userservice`

**Responsibilities:** User registration, authentication (JWT), profile management, wallet management, KYC status, gRPC server for other services.

#### Models

- **`Users.java`** - JPA entity with UUID id, username, email, passwordHash, fullName, phone, kycStatus (PENDING/VERIFIED/REJECTED), role (USER/SELLER/ADMIN), timestamps, `@Version` for optimistic locking.
- **`Wallet.java`** - JPA entity with UUID id, userId (FK to Users), balance (BigDecimal), reserveBalance, currency, `@Version`.
- **`RefreshToken.java`** - JPA entity for refresh token rotation: UUID id, userId, token string, expiresAt, revoked flag.

#### Authentication Flow

```
Register:
  POST /api/auth/register
  → Check email uniqueness (existsByEmail)
  → BCrypt hash password
  → Save Users entity
  → Create Wallet (zero balance)
  → Generate JWT access + refresh tokens
  → Store RefreshToken in DB
  → Return AuthResponseDTO

Login:
  POST /api/auth/login
  → Find user by email
  → Verify password with BCrypt
  → Generate new tokens
  → Store refresh token
  → Return AuthResponseDTO

Refresh:
  POST /api/auth/refresh
  → Find RefreshToken by token string
  → Check not revoked AND not expired
  → Revoke old token (set revoked=true)
  → Generate new token pair
  → Store new refresh token
  → Return AuthResponseDTO
```

#### Security Configuration

- `SecurityConfig.java`: Spring Security filter chain
  - `/api/auth/**` - permitAll (no auth needed)
  - All other endpoints - authenticated
  - Stateless sessions (no HTTP session)
  - CSRF disabled (stateless API)
  - CORS for `localhost:5173` (Vite dev server)
  - BCryptPasswordEncoder bean
  - `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`

- `JwtTokenProvider.java`: JWT creation and validation
  - HMAC key from `jwt.secret` property
  - Access token: userId (subject) + role claim + expiration
  - Refresh token: userId + longer expiration
  - `validateToken()` catches `JwtException`

- `JwtAuthenticationFilter.java`: `OncePerRequestFilter`
  - Extracts `Bearer <token>` from Authorization header
  - Validates JWT
  - Sets `SecurityContext` with `UsernamePasswordAuthenticationToken`

#### gRPC Server (`UserServiceGrpc.java`)

Implements 3 RPCs:
- `GetUserProfile(userId)` → returns `UserProfile` protobuf
- `GetWalletBalance(userId)` → returns `WalletBalance` protobuf
- `UpdateWallet(userId, amount, type, idempotencyKey)` → credits/debits wallet, returns new balance

---

### 4.2 Bidding Service (Port 8001/9001)

**Package:** `com.vaultx.bidding`

**Responsibilities:** Auction CRUD, bid placement, auction lifecycle scheduling, Kafka event publishing via outbox pattern, gRPC server for auction details.

#### Models

- **`Auction.java`** - UUID id, title, description, sellerId, startingPrice, reservePrice, currentBid, bidIncrement, status (PENDING/ACTIVE/SOLD/UNSOLD/CANCELLED), startTime, endTime, extendedAt, extensionPeriodSeconds (default 120s), currency, `@Version`.
- **`Bid.java`** - UUID id, auctionId, bidderId, amount, maxAutoBid, isAutoBid, status (ACTIVE/OUTBID/WINNING/WON/LOST), `idempotencyKey` (UNIQUE constraint).
- **`OutboxEvent.java`** - UUID id, aggregateType, aggregateId, eventType, payload (TEXT), published (boolean), publishedAt.

#### Bid Placement Flow (`BidService.placeBid()`)

```
POST /api/auctions/{id}/bids
1. Idempotency check - reject duplicate idempotencyKey
2. gRPC call to User Service → get wallet balance
3. Validate: availableBalance = balance - reservedBalance >= bid amount
4. Validate: if maxAutoBid set, also check against availableBalance
5. Fetch auction with optimistic lock (findByIdWithLock → @Lock(PESSIMISTIC_WRITE))
6. Validate auction: status == ACTIVE, not expired, bidder != seller
7. Validate bid amount >= minimum (currentBid + increment, or startingPrice)
8. Mark previous winning bids as OUTBID
9. Save new Bid with status WINNING
10. Update auction.currentBid
11. Create OutboxEvent (BID_PLACED) in same transaction
12. Return BidResponse
```

#### Auction Lifecycle Scheduler (`AuctionScheduler`)

Runs every 30 seconds (`@Scheduled(fixedRate = 30000)`):

```
Process PENDING → ACTIVE:
  Query: findPendingToStart(now)
  For each: set status=ACTIVE, emit AUCTION_STARTED outbox event

Process ACTIVE → SOLD/UNSOLD:
  Query: findActiveToEnd(now)
  For each:
    - Check reserve price met
    - Set status = SOLD (if bids + reserve met) or UNSOLD
    - Find top bidder
    - Emit AUCTION_ENDED outbox event
    - If SOLD: also emit AUCTION_WON outbox event
```

#### Outbox Poller (`OutboxPoller`)

Runs every 5 seconds (`@Scheduled(fixedDelay = 5000)`):

```
1. Query: findUnpublished() → SELECT * WHERE published=false LIMIT 100
2. For each event:
   - Map eventType to Kafka topic (e.g., BID_PLACED → "bid.placed")
   - Deserialize payload to typed DTO
   - Send to Kafka via KafkaTemplate
   - On success: mark published=true, set publishedAt
   - On failure: log error (event stays unpublished for retry)
```

#### Kafka Topics (12 total)

| Topic | Partitions | Purpose |
|---|---|---|
| `bid.placed` | 6 | New bid events |
| `auction.created` | 6 | Auction creation |
| `auction.started` | 6 | Auction goes active |
| `auction.ended` | 6 | Auction closed |
| `auction.won` | 6 | Winner notification |
| `auction.lost` | 6 | Loser notification |
| `bid.placed.retry` | 3 | Retry for failed bid events |
| `auction.won.retry` | 3 | Retry for failed won events |
| `auction.ended.retry` | 3 | Retry for failed ended events |
| `bid.placed.dlq` | 1 | Dead letter queue |
| `auction.won.dlq` | 1 | Dead letter queue |
| `auction.ended.dlq` | 1 | Dead letter queue |

#### gRPC Client (`UserGrpcClient.java`)

- `@GrpcClient("user-service")` - connects to User Service gRPC on port 9000
- Uses blocking stub for synchronous calls
- Methods: `getUserProfile(userId)`, `getWalletBalance(userId)`

#### gRPC Server (`BiddingGrpcService.java`)

- Implements `GetAuctionDetails` RPC
- Other services (Transaction Service) can query auction info

---

### 4.3 Transaction Service (Port 8002/9002)

**Package:** `com.pm.transactionservice`

**Responsibilities:** Payment processing, escrow management, refunds (partially implemented).

**Current State:**
- Kafka configuration and topic declarations (same 12 topics)
- `OutboxEvent` model and repository
- Event DTOs for deserialization (`AuctionCreatedEvent`, `AuctionEndedEvent`, `BidPlacedEvent`)
- No controllers or consumer logic implemented yet
- gRPC client stubs for User Service and Bidding Service

---

### 4.4 Frontend (React SPA, Port 3000)

**Package:** `frontend/`

A fully functional SPA using mock data (no backend API integration).

#### Pages

| Page | Route | Description |
|---|---|---|
| Landing | `/` | Marketing page with hero, features, pricing |
| Login | `/login` | Email/password auth form |
| Register | `/register` | User registration with password strength |
| Home | `/home` | Authenticated dashboard with metrics |
| Explore | `/explore` | Browse auctions with search/filter/sort |
| Auction Detail | `/auction/:id` | Full auction view with bid placement |
| Wallet | `/wallet` | Balance, deposit, KYC verification wizard |
| Transactions | `/transactions` | Transaction history with CSV export |
| Seller Portal | `/seller` | Manage listings, create auctions |
| Checkout | `/checkout` | Post-auction settlement |

#### State Management

- `api.ts` contains all mock data (MOCK_USER, MOCK_AUCTIONS, MOCK_BID_HISTORY, MOCK_TRANSACTIONS)
- State persisted to `localStorage` via `saveState()`
- No Redux/Zustand - component-level state with `useState`/`useEffect`

---

## 5. Core Workflows

### 5.1 User Registration & Authentication

```
Client                User Service              PostgreSQL
  │                       │                         │
  │ POST /api/auth/register                       │
  │──────────────────────►│                         │
  │                       │ Check email unique      │
  │                       │────────────────────────►│
  │                       │◄────────────────────────│
  │                       │                         │
  │                       │ Hash password (BCrypt)  │
  │                       │ Save Users entity       │
  │                       │────────────────────────►│
  │                       │                         │
  │                       │ Create Wallet (0 balance)│
  │                       │────────────────────────►│
  │                       │                         │
  │                       │ Generate JWT pair       │
  │                       │ Store RefreshToken      │
  │                       │────────────────────────►│
  │                       │                         │
  │◄──────────────────────│ AuthResponseDTO         │
  │  {accessToken,        │                         │
  │   refreshToken}       │                         │
```

### 5.2 Auction Creation

```
Client            Bidding Service           PostgreSQL          Kafka
  │                     │                       │                  │
  │ POST /api/auctions  │                       │                  │
  │────────────────────►│                       │                  │
  │                     │ Validate seller       │                  │
  │                     │ Save Auction (PENDING)│                  │
  │                     │──────────────────────►│                  │
  │                     │                       │                  │
  │                     │ Create OutboxEvent    │                  │
  │                     │ (AUCTION_CREATED)     │                  │
  │                     │──────────────────────►│                  │
  │                     │                       │                  │
  │◄────────────────────│ AuctionResponse       │                  │
  │                     │                       │                  │
  │                     │ [OutboxPoller runs]   │                  │
  │                     │─────────────────────────────────────────►│
  │                     │                       │   auction.created│
```

### 5.3 Bid Placement (Critical Path)

```
Client          Bidding Service        User Service(gRPC)      Kafka
  │                   │                      │                    │
  │ POST /api/        │                      │                    │
  │ auctions/{id}/bids│                      │                    │
  │──────────────────►│                      │                    │
  │                   │                      │                    │
  │                   │ 1. Idempotency check │                    │
  │                   │    (DB lookup)       │                    │
  │                   │                      │                    │
  │                   │ 2. GetWalletBalance  │                    │
  │                   │─────────────────────►│                    │
  │                   │◄─────────────────────│                    │
  │                   │  {balance, reserved} │                    │
  │                   │                      │                    │
  │                   │ 3. Validate balance  │                    │
  │                   │    >= bid amount     │                    │
  │                   │                      │                    │
  │                   │ 4. Fetch auction     │                    │
  │                   │    (PESSIMISTIC LOCK)│                    │
  │                   │──────────────────────│                    │
  │                   │                      │                    │
  │                   │ 5. Validate:         │                    │
  │                   │    - status=ACTIVE   │                    │
  │                   │    - not expired     │                    │
  │                   │    - bidder != seller│                    │
  │                   │    - amount >= min   │                    │
  │                   │                      │                    │
  │                   │ 6. Mark old bids     │                    │
  │                   │    OUTBID            │                    │
  │                   │──────────────────────│                    │
  │                   │                      │                    │
  │                   │ 7. Save new Bid      │                    │
  │                   │    (status=WINNING)  │                    │
  │                   │──────────────────────│                    │
  │                   │                      │                    │
  │                   │ 8. Update auction    │                    │
  │                   │    currentBid        │                    │
  │                   │──────────────────────│                    │
  │                   │                      │                    │
  │                   │ 9. Create OutboxEvent│                    │
  │                   │    (BID_PLACED)      │                    │
  │                   │──────────────────────│                    │
  │                   │                      │                    │
  │◄──────────────────│ BidResponse          │                    │
  │                   │                      │                    │
  │                   │ [OutboxPoller]       │                    │
  │                   │───────────────────────────────────────────►│
  │                   │                      │     bid.placed     │
```

### 5.4 Auction Lifecycle (Scheduler-Driven)

```
Every 30 seconds, AuctionScheduler runs:

  ┌─────────────────────────────────────────────────────┐
  │ 1. FIND PENDING AUCTIONS where startTime <= now     │
  │    → Set status = ACTIVE                            │
  │    → Create AUCTION_STARTED outbox event            │
  └─────────────────────────────────────────────────────┘
                          │
                          ▼
  ┌─────────────────────────────────────────────────────┐
  │ 2. FIND ACTIVE AUCTIONS where endTime <= now        │
  │    → Check reserve price                            │
  │    → Set status = SOLD or UNSOLD                    │
  │    → Find top bidder                                │
  │    → Create AUCTION_ENDED outbox event              │
  │    → If SOLD: create AUCTION_WON outbox event       │
  └─────────────────────────────────────────────────────┘
                          │
                          ▼
  ┌─────────────────────────────────────────────────────┐
  │ 3. OUTBOX POLLER (every 5s)                         │
  │    → Read unpublished events                        │
  │    → Publish to correct Kafka topic                 │
  │    → Mark as published                              │
  └─────────────────────────────────────────────────────┘
```

### 5.5 Payment Settlement (Planned)

```
Transaction Service (Kafka Consumer):

  Receives auction.won event
  → Debit winner's wallet (hold in escrow)
  → On delivery confirmation:
      → Release escrow to seller
      → Emit PAYMENT_COMPLETED event
  → On dispute:
      → Refund to buyer
      → Emit PAYMENT_REFUNDED event
```

---

## 6. Data Flow Diagrams

### Synchronous Communication (gRPC)

```
Bidding Service ──gRPC──► User Service
  - getWalletBalance(userId) → {balance, reservedBalance}
  - getUserProfile(userId) → {username, email, kycStatus}

Transaction Service ──gRPC──► User Service
  - getWalletBalance(userId)
  - UpdateWallet(userId, amount, type)

Transaction Service ──gRPC──► Bidding Service
  - GetAuctionDetails(auctionId)
```

### Asynchronous Communication (Kafka)

```
Bidding Service ──Kafka──► Transaction Service
  Topics: bid.placed, auction.created, auction.started,
          auction.ended, auction.won, auction.lost

Bidding Service ──Kafka──► Notification Service (planned)
  Same topics, different consumer group
```

---

## 7. Database Schema

### User Service DB (PostgreSQL, port 5000)

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    phone VARCHAR(20),
    kyc_status VARCHAR(20) DEFAULT 'PENDING',
    role VARCHAR(20) DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT
);

CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    balance DECIMAL(15,2) DEFAULT 0,
    reserve_balance DECIMAL(15,2) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'USD',
    version BIGINT
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE
);
```

### Bidding Service DB (PostgreSQL, port 5001)

```sql
CREATE TABLE auctions (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    seller_id UUID NOT NULL,
    starting_price DECIMAL(15,2) NOT NULL,
    reserve_price DECIMAL(15,2),
    current_bid DECIMAL(15,2),
    bid_increment DECIMAL(15,2) DEFAULT 1,
    status VARCHAR(20) DEFAULT 'PENDING',
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    extended_at TIMESTAMP,
    extension_period_seconds INT DEFAULT 120,
    currency VARCHAR(3) DEFAULT 'USD',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT
);

CREATE TABLE bids (
    id UUID PRIMARY KEY,
    auction_id UUID NOT NULL,
    bidder_id UUID NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    max_auto_bid DECIMAL(15,2),
    is_auto_bid BOOLEAN DEFAULT FALSE,
    status VARCHAR(20),
    idempotency_key VARCHAR(255) UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    published BOOLEAN DEFAULT FALSE,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);
```

---

## 8. Infrastructure & Deployment

### Docker Compose Services

| Service | Image | Port | Depends On |
|---|---|---|---|
| kafka | confluentinc/cp-kafka:7.6.0 | 9092 (internal), 9094 (external) | - |
| schema-registry | confluentinc/cp-schema-registry:7.6.0 | 8081 | kafka |
| user-service-db | postgres:15-alpine | 5000 | - |
| bid-service-db | postgres:15-alpine | 5001 | - |
| user-service | Built from ./user-service | 8000 | user-service-db |
| bidding-service | Built from ./bidding-service | 8001 | bid-service-db, kafka, user-service |

### Key Configuration

- **Kafka KRaft mode** (no Zookeeper): Single-node broker+controller
- **Auto-create topics disabled** (`KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`) - topics defined in code
- **Health checks**: Kafka (topic list), PostgreSQL (pg_isready), User Service (nc -z)
- **Startup order**: Kafka → Schema Registry → PostgreSQL DBs → User Service → Bidding Service
- **Multi-stage Dockerfiles**: Maven build stage → JDK 21 JRE Alpine runtime

---

## 9. Security Architecture

### JWT Flow

```
Client stores:
  - accessToken (in memory/localStorage)
  - refreshToken (in memory/localStorage)

Request flow:
  Authorization: Bearer <accessToken>
       │
       ▼
  JwtAuthenticationFilter
       │
       ├─ Extract token from header
       ├─ jwtTokenProvider.validateToken(token)
       ├─ jwtTokenProvider.getUserIdFromToken(token)
       ├─ Load SecurityContext
       └─ Continue filter chain
```

### Token Details

- **Access Token**: Short-lived (configurable via `jwt.access-token-expiration`), contains userId + role
- **Refresh Token**: Longer-lived (7 days stored in DB), supports rotation (old revoked on use)
- **Signing**: HMAC using `jwt.secret` from `application.properties`

### Role-Based Access

- `SecurityConfig` permits `/api/auth/**` without authentication
- All other endpoints require authentication
- Role checks done at service level (not enforced at URL level currently)

---

## 10. Current Status & Gaps

### Implemented

- [x] User Service: Full auth, profile CRUD, wallet, gRPC server
- [x] Bidding Service: Full auction CRUD, bid placement, scheduler, outbox, gRPC
- [x] Transaction Service: Kafka config, topic declarations, event DTOs
- [x] Frontend: Complete SPA with mock data (10 pages)
- [x] Docker Compose: Kafka, Schema Registry, 2 PostgreSQL DBs, 2 services
- [x] Unit tests for user-service (auth, user, wallet services + controllers + repos)
- [x] Unit tests for bidding-service (auction, bid services + controllers + repos)

### Not Yet Implemented

- [ ] Transaction Service: Consumer logic, payment processing, escrow, refunds
- [ ] Frontend API integration (currently uses mock data)
- [ ] API Gateway (documented in architecture.md but not built)
- [ ] Notification Service (email/SMS/push)
- [ ] Auto-bid engine (bid.autoBid field exists but no auto-bid processing)
- [ ] Soft-close extension logic in BidService (extensionPeriodSeconds field exists)
- [ ] transaction-service-db in docker-compose.yml
- [ ] Frontend service in docker-compose.yml
- [ ] Observability stack (Prometheus, Grafana, distributed tracing)
- [ ] Rate limiting, circuit breaker patterns
