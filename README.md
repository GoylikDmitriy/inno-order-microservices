# Inno Order Microservices

A production-ready microservices system built with Spring Boot, covering the full lifecycle of an e-commerce platform — from user registration and authentication to order management, payment processing, and event-driven communication.

---

## Architecture Overview

```
                          ┌─────────────────────────────────────┐
                          │            API Gateway              │
                          │  - JWT validation                   │
                          │  - Routing                          │
                          │  - Circuit Breaker (Resilience4j)   │
                          └──────────────┬──────────────────────┘
                                         │
          ┌──────────────┬───────────────┼───────────────┬──────────────────┐
          ▼              ▼               ▼               ▼                  ▼
   ┌─────────────┐ ┌──────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
   │ User Service│ │  Auth    │ │Order Service │ │Payment Svc   │ │  [Internal]  │
   │             │ │ Service  │ │              │ │              │ │  Direct k8s  │
   │ PostgreSQL  │ │          │ │  PostgreSQL  │ │   MongoDB    │ │  service DNS │
   │ Redis       │ │PostgreSQL│ │              │ │              │ └──────────────┘
   └─────────────┘ └──────────┘ └──────┬───────┘ └───────┬──────┘
                                       │  Kafka          │
                                       └────────────────►┘
                                         payment.created
```

---

## Services

| Service | Port | Description | Database |
|---|---|---|---|
| `api-gateway` | 8080 | Single entry point, JWT auth, routing | — |
| `auth-service` | 8082 | Login, token issuance, credential storage | PostgreSQL |
| `user-service` | 8081 | User profiles, payment cards | PostgreSQL + Redis |
| `order-service` | 8083 | Order management, items catalog | PostgreSQL |
| `payment-service` | 8084 | Payment processing, statistics | MongoDB |

---

## Tech Stack

### Backend
- **Java 21**
- **Spring Boot** — web, security, data, validation
- **Spring Cloud Gateway** (WebFlux) — reactive API gateway
- **Spring Cloud OpenFeign** — declarative HTTP clients for inter-service calls
- **Apache Kafka** — event-driven communication between services

### Security
- **JWT**
- **Spring Security**
- **BCrypt**
- **Internal API Key** — service-to-service call protection

### Databases
- **PostgreSQL** — user, auth, order services
- **MongoDB** — payment documents
- **Redis** — caching (user profiles, cards)
- **Liquibase** — schema migrations for PostgreSQL

### Resilience
- **Resilience4j** — circuit breaker, time limiter on all routes
- **Feign Fallback Factory** — graceful degradation on service unavailability
- **Dead Letter Topic** — failed Kafka messages with retry (3 attempts, 1s backoff)

### Observability
- **Spring Boot Actuator** — liveness and readiness probes for Kubernetes

### Infrastructure
- **Docker** + **Docker Compose** — local development
- **Kubernetes** (Minikube) — production deployment
- **Kustomize** — environment-specific overlays (dev/prod)
- **Nginx Ingress** — external traffic entry point

### CI/CD & Quality
- **GitHub Actions** — CI pipeline (build → test → SonarCloud → Docker push)
- **SonarCloud** — static code analysis and coverage
- **JaCoCo** — code coverage reports
- **Testcontainers** — integration tests with real PostgreSQL, MongoDB, Redis
- **EmbeddedKafka** — Kafka consumer integration tests

---

## Key Design Decisions

### Authentication & Authorization
```
Client → Gateway (validates JWT) → X-User-Id + X-User-Role headers → Services
Services → HeaderAuthFilter reads headers → SecurityContext → @PreAuthorize
```
JWT is parsed **only at the Gateway**. Downstream services trust forwarded headers, protected by an internal API key from direct bypass.

### Registration — Compensation Transaction (Saga)
```
POST /api/users/register
  → user-service saves user
  → user-service calls auth-service via Feign (save credentials)
  → if auth-service fails → @Transactional rolls back user save
```

### Internal Service Communication
```
order-service → GET http://gateway:8080/api/users/internal?email=...
              + X-Internal-Api-Key header
Gateway validates API key → routes to user-service → returns user info
```
Order service calls user-service **through the Gateway** to enrich order responses with user info.

### Payment Processing — Event-Driven
```
POST /api/payments
  → calls random.org API → even number = SUCCESS, odd = FAILED
  → saves to MongoDB
  → publishes PaymentCreatedEvent to Kafka topic payment.created

order-service KafkaListener:
  → SUCCESS → order status = PAID
  → FAILED  → order status = PAYMENT_FAILED
```

### Pessimistic Locking — Card Limit
Prevents race conditions when multiple requests try to create cards simultaneously for the same user. The user row is locked for the duration of the transaction.

### N+1 Prevention
All entity relationships use `JOIN FETCH` queries or `@EntityGraph` for paginated results to avoid the N+1 problem across cards, orders, and order items.

---

## Getting Started

### Run with Docker Compose

```bash
# clone the repo
git clone https://github.com/GoylikDmitriy/inno-order-microservices.git
cd inno-order-microservices

# create env with your values

# start everything
docker compose up --build
```

Services will be available at:
- Gateway: `http://localhost:8080`
- Kafka UI: `http://localhost:8090`

### Run on Kubernetes (Minikube)

```bash
# start minikube
minikube start --memory=6144 --cpus=4

# enable ingress
minikube addons enable ingress

# apply manifests
kubectl apply -k k8s-manifests/overlays/dev

# add local DNS
echo "$(minikube ip) inno.local" | sudo tee -a /etc/hosts

# access the system
curl http://inno.local/api/auth/login
```

---

## API Overview

### Authentication
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | Public | Login, returns JWT tokens |
| `POST` | `/api/auth/refresh` | Public | Refresh access token |
| `POST` | `/api/users/register` | Public | Register new user |

### Users
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/users/{id}` | ADMIN or owner | Get user by ID |
| `GET` | `/api/users` | ADMIN | Get all users (paginated, filterable) |
| `PUT` | `/api/users/{id}` | ADMIN or owner | Update user |
| `DELETE` | `/api/users/{id}` | ADMIN or owner | Soft delete user |
| `PATCH` | `/api/users/{id}/activate` | ADMIN | Activate user |
| `PATCH` | `/api/users/{id}/deactivate` | ADMIN | Deactivate user |

### Cards
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/cards` | ADMIN or owner | Create payment card (max 5 per user) |
| `GET` | `/api/cards/{id}` | ADMIN or owner | Get card by ID |
| `GET` | `/api/cards/users/{userId}` | ADMIN or owner | Get all cards for user |
| `PUT` | `/api/cards/{id}` | ADMIN or owner | Update card |
| `DELETE` | `/api/cards/{id}` | ADMIN or owner | Soft delete card |

### Orders
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/orders` | ADMIN or owner | Create order |
| `GET` | `/api/orders/{id}` | ADMIN or owner | Get order with user info |
| `GET` | `/api/orders` | ADMIN | Get all orders (filter by date range, status) |
| `GET` | `/api/orders/users/{userId}` | ADMIN or owner | Get orders by user |
| `PUT` | `/api/orders/{id}` | ADMIN or owner | Update order (status/items) |
| `DELETE` | `/api/orders/{id}` | ADMIN or owner | Soft delete order |

### Payments
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/payments` | ADMIN or owner | Process payment (random.org determines outcome) |
| `GET` | `/api/payments` | ADMIN or owner | Get payments by userId / orderId / status |
| `GET` | `/api/payments/stats/total/me` | USER or ADMIN | Total payments for current user in date range |
| `GET` | `/api/payments/stats/total` | ADMIN | Total payments for all users in date range |

---

## Project Structure

```
inno-order-microservices/
├── api-gateway/          # Spring Cloud Gateway + WebFlux
├── auth-service/         # JWT issuance + credential storage
├── user-service/         # User profiles + payment cards
├── order-service/        # Orders + items + Kafka consumer
├── payment-service/      # Payment processing + MongoDB + Kafka producer
└── k8s-manifests/
    ├── base/             # Base Kubernetes manifests
    └── overlays/
        └── dev/          # Dev environment overrides (Kustomize)
