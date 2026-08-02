# Vaultx — Real-Time Bidding Platform

Vaultx is a production-grade, event-driven Real-Time Bidding Platform built with a microservices architecture using Java 21, Spring Boot 3, Apache Kafka, gRPC, and PostgreSQL.

## 🚀 Key Features

- **Microservices Architecture:** 5 independent services (API Gateway, User Service, Bidding Service, Transaction Service, Notification Service) with database-per-service isolation.
- **Real-Time Bidding Engine:** High-performance bid placement supporting 500+ bids/second with auto-extensions (soft-close) and optimistic locking.
- **gRPC & Protobuf:** Fast, type-safe inter-service RPC communication.
- **Event-Driven Architecture:** Transactional Outbox pattern, Apache Kafka event streams, and DLQ retry strategy.
- **Secure Wallets:** Financial transactions with reserved balances to prevent double-spending during active bids.

---

## 🛠️ Tech Stack

- **Backend:** Java 21, Spring Boot 3, Spring Cloud Gateway, Spring Security
- **Database & Cache:** PostgreSQL, Redis
- **Messaging:** Apache Kafka (KRaft mode)
- **Communication:** gRPC, Protocol Buffers, REST
- **Observability:** Prometheus, Grafana, Zipkin
- **DevOps:** Docker, Docker Compose

---

## 📂 Project Structure

- `architecture.md` - Complete systems and architecture blueprint.
- `workflows.md` - End-to-end user and program workflows.
- `WORKFLOW.md` - Workflow & code explanation.
- `INTERVIEW_QA.md` - Backend interview Q&A set.
- `pom.xml` - Parent maven configuration.
- `user-service/` - Microservice handling user registry, authentication, and wallets.
- `README.md` - Quick start guide (this file).

---

## 🚦 Quick Start (Development Status)

This project is currently in the initial setup and scaffolding phase.

1. **Clone the Repository:**
   ```bash
   git clone <repository-url>
   cd Vaultx
   ```

2. **Build the Project:**
   ```bash
   mvn clean install
   ```
