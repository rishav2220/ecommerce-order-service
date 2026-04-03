# E-Commerce Order Management Microservice

A production-grade **Order Management Service** built with **Spring Boot 3**, **PostgreSQL**, **Apache Kafka**, and **Redis**. Designed to handle high-throughput order processing in a distributed e-commerce platform.

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2, Java 21 |
| Database | PostgreSQL 16 + Flyway migrations |
| Caching | Redis (lettuce client, connection pooling) |
| Messaging | Apache Kafka (idempotent producer, exactly-once semantics) |
| Security | Spring Security + JWT (RS256) |
| API Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Metrics | Micrometer + Prometheus |
| Testing | JUnit 5, Mockito, Testcontainers |
| Build | Maven, Docker, Docker Compose |

## Architecture

```
┌─────────────┐     REST      ┌──────────────────┐
│   API GW /  │ ──────────▶  │  Order Service   │
│   Client    │               │  (Spring Boot)   │
└─────────────┘               └────────┬─────────┘
                                       │
              ┌────────────────────────┼──────────────────┐
              │                        │                  │
              ▼                        ▼                  ▼
        ┌──────────┐           ┌──────────────┐   ┌────────────┐
        │PostgreSQL│           │    Redis      │   │   Kafka    │
        │(Primary  │           │   (Cache)     │   │  (Events)  │
        │ storage) │           │               │   │            │
        └──────────┘           └──────────────┘   └────────────┘
```

## Key Features

- **State Machine** — strict order lifecycle transitions with business rule enforcement
- **Optimistic Locking** — `@Version` on `Order` entity prevents lost-update race conditions
- **Redis Caching** — 30-minute TTL cache with automatic eviction on updates
- **Kafka Event Publishing** — idempotent producer with `acks=all` for order lifecycle events
- **JWT Authentication** — stateless, role-based access control (`USER` / `ADMIN`)
- **RFC 7807 Problem Details** — structured error responses with `ProblemDetail`
- **Database Migrations** — versioned schema changes via Flyway
- **Batch JPA** — configures Hibernate batch inserts/updates for performance
- **Prometheus Metrics** — all endpoints emit Micrometer metrics for monitoring

## API Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `POST` | `/api/v1/orders` | Create new order | JWT |
| `GET` | `/api/v1/orders/{id}` | Get order by ID | JWT |
| `GET` | `/api/v1/orders/customer/{customerId}` | List customer orders (paged) | JWT |
| `GET` | `/api/v1/orders/status/{status}` | List orders by status (paged) | ADMIN |
| `PATCH` | `/api/v1/orders/{id}/status` | Update order status | JWT |
| `DELETE` | `/api/v1/orders/{id}` | Cancel order | JWT |

Full Swagger UI at `http://localhost:8080/swagger-ui.html`

## Order Lifecycle

```
PENDING → CONFIRMED → PAYMENT_PROCESSING → PAID → PREPARING → SHIPPED → DELIVERED
                                        ↘ PAYMENT_FAILED
PENDING / CONFIRMED / PAID → CANCELLED
DELIVERED → REFUNDED
```

## Running Locally

### Prerequisites
- Java 21+
- Docker & Docker Compose

### Start all services

```bash
docker-compose up -d
```

This starts PostgreSQL, Redis, Kafka, and the Order Service.

### Run only infrastructure

```bash
docker-compose up -d postgres redis kafka
./mvnw spring-boot:run
```

### Run tests

```bash
./mvnw test
```

Tests use Testcontainers — Docker is required.

## Configuration

All configuration is in `src/main/resources/application.yml`. Key environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/order_db` | PostgreSQL URL |
| `DB_USERNAME` | `postgres` | DB username |
| `DB_PASSWORD` | `postgres` | DB password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker |
| `JWT_SECRET` | *(required in prod)* | JWT signing secret (256-bit min) |
| `PORT` | `8080` | Application port |

## Project Structure

```
src/
├── main/java/com/rishav/orderservice/
│   ├── cache/          # Redis cache layer
│   ├── config/         # Spring configs (Security, Kafka, Redis)
│   ├── controller/     # REST controllers
│   ├── dto/            # Request / Response DTOs with validation
│   ├── entity/         # JPA entities + OrderStatus state machine
│   ├── event/          # Kafka event models + publisher
│   ├── exception/      # Custom exceptions + GlobalExceptionHandler
│   ├── repository/     # Spring Data JPA repositories
│   └── service/        # Service interfaces + implementations
├── main/resources/
│   ├── application.yml
│   └── db/migration/   # Flyway SQL migrations
└── test/               # Unit + integration tests
```
