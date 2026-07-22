# Architecture Overview

## Domain-Driven Decomposition

The platform is decomposed into six bounded contexts, each owned by an independent service
with its own database:

| Bounded Context | Service | Aggregates | Database |
|---|---|---|---|
| Catalog | product-service | Product | productdb |
| Ordering | order-service | Order, OrderItem | orderdb |
| Payments | payment-service | Payment | paymentdb |
| Identity | user-service | User | userdb |
| Stock | inventory-service | InventoryItem | inventorydb |
| Communications | notification-service | Notification | notificationdb |

Two platform services support the mesh: **service-discovery** (Eureka) for registry/lookup and
**config-server** (Spring Cloud Config) for centralized, environment-specific configuration.
All external traffic enters through the **api-gateway**.

## Communication Patterns

- **Synchronous** (REST via the gateway, Feign between services): used for direct
  queries where the caller needs an immediate answer, e.g. a customer fetching an order status.
- **Asynchronous** (Kafka domain events): used for cross-service workflows where the caller
  doesn't need to block on the result — order fulfillment, notifications, inventory updates.

## The Order Saga

Placing an order is a distributed transaction spanning three services. It's implemented as an
**orchestrated Saga** owned by `order-service`:

```
1. order-service      CREATE_ORDER (status=PENDING)        --> emits OrderCreated
2. inventory-service  RESERVE_INVENTORY                     --> emits InventoryReserved | InventoryUnavailable
3. payment-service     PROCESS_PAYMENT                       --> emits PaymentCompleted | PaymentFailed
4. order-service      CONFIRM_ORDER (status=CONFIRMED)      --> emits OrderConfirmed
```

If any step fails, `order-service` triggers **compensating transactions**:
- Payment failure → release reserved inventory, mark order CANCELLED.
- Inventory unavailable → mark order CANCELLED (nothing to release yet).
- Either path → `notification-service` informs the customer.

This keeps each service's local transaction ACID while the overall workflow stays eventually
consistent — no service ever needs a distributed (two-phase-commit) transaction.

## Event Catalog

| Topic | Producer | Consumers | Key Events |
|---|---|---|---|
| order-events | order-service | inventory-service, payment-service, notification-service | OrderCreated, OrderConfirmed, OrderCancelled |
| inventory-events | inventory-service | order-service, payment-service | InventoryReserved, InventoryUnavailable, StockReserved, StockReleased, LowStockWarning |
| payment-events | payment-service | order-service, notification-service | PaymentCompleted, PaymentFailed, RefundProcessed |
| product-events | product-service | notification-service | ProductCreated, ProductPriceChanged, ProductOutOfStock |
| user-events | user-service | notification-service | UserRegistered, UserLoggedIn |

Every service applies the **idempotent consumer** pattern (dedupe by event id) and a
**dead-letter queue** for messages that repeatedly fail processing, so a poison message
doesn't stall a whole partition.

## Resilience

- **Circuit breakers** (Resilience4j) wrap calls at the gateway and between services; a service
  that's failing gets fast-failed rather than piling up latency downstream.
- **Retries** with exponential backoff cover transient network errors, never business exceptions.
- **Rate limiting** at the gateway protects downstream services from traffic spikes, tuned
  per-route (Product API 10 rps, Order API 5 rps, Payment API 3 rps).
- **Database-per-service** means no service's schema change or outage can directly break another.

## Observability

- **Distributed tracing**: every service propagates a trace/span id (Micrometer Tracing +
  Zipkin) so a single request across 4-5 services can be followed end-to-end.
- **Metrics**: each service exposes `/actuator/prometheus`; Grafana dashboards cover service
  health, business KPIs (orders/min, revenue), Kafka lag, and infrastructure usage.
- **Logging**: structured logs (with trace/span ids) ship to the ELK stack for correlation
  with traces and metrics.

## Deployment Topology

Locally, everything runs via `docker-compose.yml` in `infrastructure/`. For production,
`infrastructure/k8s/helm` deploys each service as its own Kubernetes Deployment + Service,
with liveness/readiness probes on `/actuator/health/{liveness,readiness}` and a
HorizontalPodAutoscaler scaling on CPU/memory. See `deployment-guide.md` for the full
walkthrough.
