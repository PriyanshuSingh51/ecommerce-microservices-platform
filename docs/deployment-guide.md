# Deployment Guide

## Prerequisites

- Java 17+, Maven 3.9+
- Docker & Docker Compose v2
- (For Kubernetes) a cluster (kind/minikube/EKS/GKE/AKS), `kubectl`, and `helm` 3.x

## 1. Local Development with Docker Compose

```bash
# Build every service's jar
./scripts/build-all.sh

# Start the full platform (databases, Kafka, all services, observability stack)
cd infrastructure
docker compose up -d --build
```

Verify everything came up:

```bash
curl http://localhost:8761                 # Eureka dashboard — all 6 services should register
curl http://localhost:8080/api/products     # Through the gateway
open http://localhost:3000                  # Grafana, admin/admin
open http://localhost:9411                  # Zipkin traces
open http://localhost:5601                  # Kibana logs
```

Tear down:

```bash
docker compose down -v   # -v also removes the named volumes (DB data, Kafka data)
```

### Running an individual service against the shared infra

Only want to iterate on `product-service` locally without rebuilding its container each time?

```bash
docker compose up -d postgres-product kafka eureka-server config-server
cd ../product-service
mvn spring-boot:run
```

## 2. Kubernetes Deployment (Helm)

```bash
# 1. Build & push images to your registry (replace 'ecommerce' with your registry)
for svc in api-gateway service-discovery config-server product-service order-service \
           payment-service user-service inventory-service notification-service; do
  docker build -t ecommerce/$svc:1.0.0 ./$svc
  docker push ecommerce/$svc:1.0.0
done

# 2. Apply namespaces, configmaps, secrets, then install the Helm chart
./scripts/deploy.sh ecommerce
```

Check rollout status:

```bash
kubectl -n ecommerce get pods
kubectl -n ecommerce get hpa
kubectl -n ecommerce logs deploy/order-service -f
```

### Scaling

The Helm chart ships a `HorizontalPodAutoscaler` per service (see `values.yaml`), scaling on
CPU (70%) and memory (80%) between 1 and 5 replicas by default. Adjust per-environment in
`infrastructure/k8s/helm/values.yaml` or via `--set`:

```bash
helm upgrade ecommerce infrastructure/k8s/helm -n ecommerce \
  --set autoscaling.maxReplicas=10
```

### Service Mesh (optional)

For advanced traffic management (canary releases, mutual TLS, fine-grained retries), layer
Istio on top: apply `VirtualService`/`DestinationRule` resources per service as shown in the
architecture doc's Istio example, targeting the same Kubernetes Service names the Helm chart
creates.

## 3. Configuration Management

`config-server` serves per-service YAML from `config-server/config-repo/`. To change a
service's configuration without rebuilding its image, edit the corresponding file there and
either restart `config-server` (native profile reloads on restart) or point it at a Git repo
in production (swap `spring.cloud.config.server.native` for `spring.cloud.config.server.git`).

## 4. Database Migrations

Each service uses `spring.jpa.hibernate.ddl-auto: update` for this local/demo scaffold. For a
production rollout, replace this with versioned migrations (Flyway or Liquibase) per service
so schema changes are explicit, reviewable, and repeatable across environments.

## 5. CI/CD (GitOps sketch)

A typical pipeline per service:

1. On PR: `mvn test` (unit tests) + `mvn package` (build check).
2. On merge to `main`: build & push a versioned Docker image.
3. A GitOps controller (Argo CD/Flux) watches a manifests repo referencing the new image tag
   and syncs it to the cluster, giving you an auditable, declarative deployment history.

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| Service not in Eureka | Wrong `EUREKA_SERVER` env var or Eureka not up yet | Check `docker compose logs eureka-server`; ensure `depends_on` ordering |
| 503 from gateway | Circuit breaker open | Check the failing service's health/logs; breaker auto-recovers after `waitDurationInOpenState` |
| Kafka consumer not receiving | Wrong `group-id` or topic name typo | Compare `application.yml` topic/group names against the event catalog in `architecture.md` |
| Order stuck in PENDING | Inventory or payment service down, or the Kafka listener isn't wired to the concrete event type | Check `order-service` logs and confirm `inventory-events`/`payment-events` consumers are up |
