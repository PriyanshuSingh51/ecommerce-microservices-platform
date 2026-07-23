# E-Commerce Microservices Platform

Event-driven, domain-driven e-commerce platform built with **Spring Boot 3.x** and **Spring Cloud**.
Nine independently deployable services communicate through an API Gateway and Apache Kafka, coordinate
distributed transactions with the **Saga pattern**, and ship with full observability, resilience, and
Kubernetes-native deployment tooling.

<p align="left">
  <img alt="Java" src="https://img.shields.io/badge/Java-17-orange">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen">
  <img alt="Spring Cloud" src="https://img.shields.io/badge/Spring%20Cloud-2023.0-brightgreen">
  <img alt="Kafka" src="https://img.shields.io/badge/Apache%20Kafka-7.4-black">
  <img alt="Docker" src="https://img.shields.io/badge/Docker-Compose-2496ED">
  <img alt="Kubernetes" src="https://img.shields.io/badge/Kubernetes-Helm-326CE5">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-lightgrey">
</p>

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Services](#services)
- [Technology Stack](#technology-stack)
- [Event-Driven Architecture](#event-driven-architecture)
- [The Order Saga](#the-order-saga)
- [Resilience & Observability](#resilience--observability)
- [Repository Structure](#repository-structure)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Testing](#testing)
- [Technical Requirements Checklist](#technical-requirements-checklist)
- [Troubleshooting](#troubleshooting)
- [Roadmap / Extension Points](#roadmap--extension-points)
- [License](#license)

---

## Overview

This project demonstrates enterprise-grade microservices architecture with a focus on **scalability,
resilience, and maintainability**. It decomposes an e-commerce domain into bounded contexts following
Domain-Driven Design, and coordinates cross-service workflows asynchronously over Kafka instead of
distributed (two-phase-commit) transactions.

**Goals:**
- Decompose the domain into independent services (Product, Order, Payment, User, Inventory, Notification), each owning its own database.
- Coordinate the order workflow with an orchestrated **Saga**, including compensating transactions on failure.
- Provide a single, resilient entry point via an **API Gateway** (routing, auth, rate limiting, circuit breaking).
- Ship full **observability**: distributed tracing, metrics, and centralized logging.
- Support both local development (**Docker Compose**) and production-style orchestration (**Kubernetes + Helm**).

## Architecture

```
                     ┌──────────────────────────────────────────┐
                     │     API Gateway (Spring Cloud Gateway)    │
                     │  Routing · Auth · Rate Limiting · CB      │
                     └───────────────────┬────────────────────--┘
                                          │
   ┌───────────────┬──────────────┬──────┼───────────┬──────────────────┬────────────────────┐
   ▼               ▼              ▼      ▼           ▼                  ▼                    ▼
Product Svc    Order Svc     Payment Svc  User Svc  Inventory Svc  Notification Svc
  :8081          :8084         :8086       :8088       :8090            :8092
   │               │              │          │           │                  │
   └───────────────┴──────────────┴──── Kafka ┴───────────┴──────────────────┘

Service Discovery (Eureka :8761) │ Config Server (:8888) │ Distributed Tracing (Zipkin :9411)
PostgreSQL (database-per-service) │ Redis (cache / rate-limit counters)
```

**Communication:**
- **Synchronous** (REST via the gateway / Feign) for direct queries that need an immediate answer.
- **Asynchronous** (Kafka domain events) for cross-service workflows — order fulfillment, notifications, inventory updates — keeping services loosely coupled and eventually consistent.

## Services

| Service | Port | Responsibility | Database |
|---|---|---|---|
| `api-gateway` | 8080 | Routing, auth, rate limiting, circuit breaking (edge service) | — |
| `service-discovery` | 8761 | Eureka service registry | — |
| `config-server` | 8888 | Centralized configuration for all services | — |
| `product-service` | 8081 | Catalog management, product search, categories | `productdb` |
| `order-service` | 8084 | Order processing, order history, **Saga orchestration** | `orderdb` |
| `payment-service` | 8086 | Payment processing, transaction management, refunds | `paymentdb` |
| `user-service` | 8088 | User management, JWT authentication, profiles | `userdb` |
| `inventory-service` | 8090 | Stock management, reservations, low-stock alerts | `inventorydb` |
| `notification-service` | 8092 | Email / SMS / push notifications | `notificationdb` |

9 deployable Spring Boot applications in total — 6 business services plus the 3 platform services.

## Technology Stack

**Core:** Java 17 · Spring Boot 3.2.x · Spring Cloud 2023.0.x (Gateway, Config, OpenFeign, Circuit Breaker)

**Messaging:** Apache Kafka 7.4 + ZooKeeper

**Persistence:** PostgreSQL 15 (database-per-service) · Redis 7 (cache / gateway rate-limit counters)

**Discovery & Config:** Netflix Eureka · Spring Cloud Config

**Containers:** Docker · Docker Compose · Kubernetes + Helm

**Observability:** Micrometer Tracing + Zipkin · Prometheus + Grafana · ELK (Elasticsearch, Logstash, Kibana) · Loki (alternative log backend)

**Resilience:** Resilience4j (circuit breaker, retry, rate limiter)

**API Docs:** SpringDoc OpenAPI (Swagger UI per service)

**Security:** Spring Security Crypto (BCrypt) · JJWT (JSON Web Tokens)

## Event-Driven Architecture

```
Order Service    → OrderCreated       → Kafka → [Inventory, Payment, Notification]
Inventory Service → InventoryReserved  → Kafka → [Order]
Payment Service   → PaymentCompleted   → Kafka → [Order, Notification]
Order Service    → OrderConfirmed / OrderCancelled → Kafka → [Notification]
```

### Domain Events

| Domain | Events |
|---|---|
| Product | `ProductCreated`, `ProductUpdated`, `ProductPriceChanged`, `ProductOutOfStock` |
| Order | `OrderCreated`, `OrderCancelled`, `OrderShipped`, `OrderDelivered`, `OrderConfirmed` |
| Payment | `PaymentInitiated`, `PaymentCompleted`, `PaymentFailed`, `RefundProcessed` |
| Inventory | `StockReserved`, `StockReleased`, `StockUpdated`, `LowStockWarning` |
| User | `UserRegistered`, `UserLoggedIn`, `ProfileUpdated`, `PasswordChanged` |

### Kafka Topics

| Topic | Partitions | Replicas | Retention |
|---|---|---|---|
| `order-events` | 3 | 2 | 7 days |
| `payment-events` | 3 | 2 | 7 days |
| `inventory-events` | 3 | 2 | 7 days |
| `product-events` | 3 | 2 | 7 days |
| `user-events` | 3 | 2 | 7 days |
| `dlq-events` | 1 | 1 | 30 days |

Every consumer applies the **idempotent consumer** pattern (dedupe by event id) and routes
repeatedly-failing messages to a **dead-letter queue** so one poison message can't stall a partition.
Event schemas are versioned as JSON Schema in [`common/event-schemas`](common/event-schemas).

## The Order Saga

Placing an order is the platform's central distributed transaction, implemented as an **orchestrated
Saga** owned by `order-service` (see `order-service/src/main/java/com/ecommerce/order/saga/OrderSagaManager.java`):

```
1. CREATE_ORDER       (order-service)      status → PENDING            → emits OrderCreated
2. RESERVE_INVENTORY  (inventory-service)  reserve stock                → emits InventoryReserved | InventoryUnavailable
3. PROCESS_PAYMENT    (payment-service)    charge customer              → emits PaymentCompleted | PaymentFailed
4. CONFIRM_ORDER      (order-service)      status → CONFIRMED           → emits OrderConfirmed
```

**Failure handling (compensating transactions):**
- Payment failed → release reserved inventory, mark order `CANCELLED`.
- Inventory unavailable → mark order `CANCELLED` (nothing to release yet).
- Either path → `notification-service` informs the customer.

Each step's local transaction commits independently — no service ever needs a two-phase-commit
transaction, and the system stays eventually consistent.

## Resilience & Observability

- **Circuit Breaker** (Resilience4j) at the gateway and between services — fails fast instead of piling up latency downstream.
- **Retry** with exponential backoff for transient errors only (never business exceptions).
- **Rate Limiting** at the gateway, tuned per route:

  | Route | Requests/sec (per user) | Burst / Retry |
  |---|---|---|
  | `/api/products/**` | 10 | burst 20 |
  | `/api/orders/**` | 5 | retry ×3 on 5xx |
  | `/api/payments/**` | 3 | burst 6 |

- **Distributed tracing** via Micrometer Tracing + Zipkin (`http://localhost:9411`) — follow a single request across every service it touches.
- **Metrics** via Prometheus (`/actuator/prometheus` on every service) + Grafana dashboards for service health, business KPIs, and Kafka lag.
- **Centralized logging** via the ELK stack, correlated with traces via trace/span ids in the log pattern.
- **Database-per-service** isolation — no service's schema change or outage can directly break another.

## Repository Structure

```
week11-ecommerce-microservices/
├── infrastructure/
│   ├── docker-compose.yml
│   ├── k8s/
│   │   ├── namespaces.yaml
│   │   ├── configmaps.yaml
│   │   ├── secrets.yaml
│   │   └── helm/                  # Chart.yaml, values.yaml, templates/
│   └── monitoring/
│       ├── prometheus.yml
│       ├── grafana-dashboards/
│       └── loki-config.yaml
├── api-gateway/                   # Spring Cloud Gateway
├── service-discovery/             # Eureka server
├── config-server/                 # Spring Cloud Config
├── product-service/
├── order-service/                 # + saga/OrderSagaManager.java
├── payment-service/
├── user-service/                  # + config/JwtTokenProvider.java
├── inventory-service/
├── notification-service/
├── common/
│   ├── common-lib/                # shared exceptions, DTOs, DomainEvent envelope
│   ├── event-schemas/             # JSON Schemas per event
│   └── api-contracts/             # openapi.yaml
├── scripts/
│   ├── build-all.sh
│   ├── deploy.sh
│   └── monitoring-setup.sh
├── docs/
│   ├── architecture.md
│   ├── api-documentation.md
│   └── deployment-guide.md
└── README.md
```

Every business service follows the same internal package convention:

```
com.ecommerce.<service>/
├── <Service>ServiceApplication.java   # Spring Boot entry point
├── controller/     REST controllers (SpringDoc-annotated)
├── service/        Business logic
├── repository/     Spring Data JPA repositories
├── model/          JPA entities
├── event/          Kafka event DTOs, producers, listeners
├── config/         Service-specific configuration (e.g. JWT provider)
└── saga/           (order-service only) Saga orchestration logic
```

## Getting Started

### Prerequisites

- Java 17+ and Maven 3.9+
- Docker & Docker Compose v2
- (For Kubernetes) a cluster (kind/minikube/EKS/GKE/AKS), `kubectl`, and `helm` 3.x

### Local Development (Docker Compose)

```bash
# 1. Build every service's jar
./scripts/build-all.sh

# 2. Start the full platform (databases, Kafka, all services, observability stack)
cd infrastructure
docker compose up -d --build

# 3. Verify
curl http://localhost:8761                # Eureka dashboard — all services should register
curl http://localhost:8080/api/products   # Through the gateway
open http://localhost:3000                # Grafana (admin/admin)
open http://localhost:9411                # Zipkin traces
open http://localhost:5601                # Kibana logs
```

Tear down:

```bash
docker compose down -v   # -v also removes DB/Kafka volumes
```

### Running a Single Service Against the Shared Infra

```bash
docker compose up -d postgres-product kafka eureka-server config-server
cd ../product-service
mvn spring-boot:run
```

### Configuration Management

`config-server` serves per-service YAML from `config-server/config-repo/`. Edit a file there and
restart `config-server` to pick up changes locally (native profile), or point it at a Git repository
in production by swapping the `native` profile for `git`.

## API Reference

All routes are exposed through the gateway at `http://localhost:8080`. Every service also serves
Swagger UI at `http://localhost:<port>/swagger-ui.html` and a spec at `/v3/api-docs`. A gateway-level
summary lives in [`common/api-contracts/openapi.yaml`](common/api-contracts/openapi.yaml).

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/products` | List products — filter by `category` or `q` (search) |
| `POST` | `/api/products` | Create a product (admin) |
| `GET` | `/api/orders` | List orders, optionally by `customerId` |
| `POST` | `/api/orders` | Create an order — starts the Saga (`202 Accepted`) |
| `POST` | `/api/orders/{id}/cancel` | Cancel an order and trigger compensations |
| `POST` | `/api/payments` | Process a payment for an order |
| `POST` | `/api/payments/{id}/refund` | Refund a completed payment |
| `POST` | `/api/auth/register` | Register a new customer |
| `POST` | `/api/auth/login` | Authenticate; returns a JWT |
| `GET` | `/api/inventory/{productId}` | Get stock for a product |
| `GET` | `/api/notifications` | List notifications, optionally by `recipientId` |

**Example — create an order:**

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
        "customerId": "cust-123",
        "items": [{ "productId": "prod-1", "quantity": 2, "price": 19.99 }],
        "totalAmount": 39.98
      }'

# → 202 Accepted
# { "orderId": "ord-7f3a9c", "status": "PENDING", "message": "Order accepted; saga in progress" }
```

## Kubernetes Deployment

```bash
# 1. Build & push images to your registry
for svc in api-gateway service-discovery config-server product-service \
           order-service payment-service user-service inventory-service \
           notification-service; do
  docker build -t ecommerce/$svc:1.0.0 ./$svc
  docker push ecommerce/$svc:1.0.0
done

# 2. Apply namespaces/configmaps/secrets, then install the Helm chart
./scripts/deploy.sh ecommerce

# 3. Check rollout status
kubectl -n ecommerce get pods
kubectl -n ecommerce get hpa
```

The Helm chart ([`infrastructure/k8s/helm`](infrastructure/k8s/helm)) ships a
`HorizontalPodAutoscaler` per service, scaling on CPU (70%) and memory (80%) between 1 and 5
replicas by default:

```bash
helm upgrade ecommerce infrastructure/k8s/helm -n ecommerce --set autoscaling.maxReplicas=10
```

## Testing

Each service includes JUnit 5 + Mockito unit tests for its core business logic:

```bash
cd product-service && mvn test
```

| Service | Test Class | Coverage |
|---|---|---|
| `product-service` | `ProductServiceTest` | Create publishes event; find-by-id miss; price-change event |
| `order-service` | `OrderSagaManagerTest` | Saga start persists `PENDING` + publishes `OrderCreated`; payment failure triggers cancellation + compensating event |
| `payment-service` | `PaymentServiceTest` | Payment processing persists amount/order id correctly |
| `user-service` | `UserServiceTest` | Duplicate email rejected; password hashed, never stored in plaintext |
| `inventory-service` | `InventoryServiceTest` | Insufficient stock publishes `InventoryUnavailable` without mutating state; sufficient stock reserves and decrements availability |

## Technical Requirements Checklist

| # | Requirement | Where |
|---|---|---|
| 1 | Spring Boot 3.x microservices (8+ services) | 9 services total — see [Services](#services) |
| 2 | Spring Cloud Gateway for API routing | `api-gateway/` |
| 3 | Service discovery with Netflix Eureka | `service-discovery/`; every service is a Eureka client |
| 4 | Event-driven communication with Apache Kafka | Kafka + ZooKeeper in `docker-compose.yml`; `event/` package per service |
| 5 | Distributed transactions with Saga pattern | `order-service/.../saga/OrderSagaManager.java` |
| 6 | Docker containerization for all services | `Dockerfile` in every service directory |
| 7 | Kubernetes deployment with Helm charts | `infrastructure/k8s/helm/` |
| 8 | Distributed tracing with Sleuth/Zipkin | Micrometer Tracing + Zipkin in every `application.yml` |
| 9 | Centralized logging with ELK stack | Elasticsearch/Logstash/Kibana in `docker-compose.yml` |
| 10 | Configuration management with Spring Cloud Config | `config-server/` |
| 11 | Resilience patterns with Resilience4j | Circuit breaker / retry / rate limiter beans |
| 12 | API documentation with SpringDoc OpenAPI | SpringDoc in every service + `common/api-contracts/openapi.yaml` |

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| Service not in Eureka | Wrong `EUREKA_SERVER` env var, or Eureka not up yet | Check `docker compose logs eureka-server`; verify `depends_on` ordering |
| 503 from gateway | Circuit breaker open | Check the failing service's logs; breaker auto-recovers after `waitDurationInOpenState` |
| Kafka consumer idle | Wrong `group-id` or topic name | Compare `application.yml` topic/group names against the event catalog above |
| Order stuck in `PENDING` | Inventory/payment service down or listener misconfigured | Check `order-service` logs; confirm `inventory-events`/`payment-events` consumers are running |

## Roadmap / Extension Points

- Swap the simulated payment gateway call in `payment-service` for a real Stripe/PayPal client.
- Replace `spring.jpa.hibernate.ddl-auto: update` with versioned Flyway/Liquibase migrations per service.
- Add OAuth2 social login on top of the existing JWT flow in `user-service`.
- Layer Istio (`VirtualService`/`DestinationRule`) over the Helm chart for canary releases and mTLS.
- Wire a GitOps pipeline (Argo CD/Flux) against the Helm chart for declarative, auditable deployments.

## License

MIT — see `LICENSE` (add your organization's preferred license file before publishing).
