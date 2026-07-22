# API Documentation

All routes below are exposed through the API Gateway at `http://localhost:8080`. Each service
also serves interactive Swagger UI directly at `http://localhost:<port>/swagger-ui.html` and a
machine-readable spec at `/v3/api-docs`. A gateway-level summary lives in
`common/api-contracts/openapi.yaml`.

## Products (`product-service`, direct port 8081)

| Method | Path | Description |
|---|---|---|
| GET | `/api/products` | List products. Supports `?category=` and `?q=` (search). |
| GET | `/api/products/{id}` | Get a single product. |
| POST | `/api/products` | Create a product (admin). |
| PUT | `/api/products/{id}` | Update a product (admin). |
| DELETE | `/api/products/{id}` | Delete a product (admin). |

## Orders (`order-service`, direct port 8084)

| Method | Path | Description |
|---|---|---|
| GET | `/api/orders` | List orders. Supports `?customerId=`. |
| GET | `/api/orders/{id}` | Get order status/details. |
| POST | `/api/orders` | Create an order; kicks off the order saga. Returns `202 Accepted`. |
| POST | `/api/orders/{id}/cancel` | Cancel an order and trigger compensations. |

Example request:
```json
POST /api/orders
{
  "customerId": "cust-123",
  "items": [
    { "productId": "prod-1", "quantity": 2, "price": 19.99 }
  ],
  "totalAmount": 39.98
}
```

## Payments (`payment-service`, direct port 8086)

| Method | Path | Description |
|---|---|---|
| GET | `/api/payments` | List payments. |
| GET | `/api/payments/order/{orderId}` | Get the payment for an order. |
| POST | `/api/payments` | Process a payment. |
| POST | `/api/payments/{id}/refund` | Refund a completed payment. |

## Auth & Users (`user-service`, direct port 8088)

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new customer. |
| POST | `/api/auth/login` | Authenticate; returns a JWT. |
| GET | `/api/users/{id}` | Get a user profile. |

## Inventory (`inventory-service`, direct port 8090)

| Method | Path | Description |
|---|---|---|
| GET | `/api/inventory` | List all stock records. |
| GET | `/api/inventory/{productId}` | Get stock for a product. |
| POST | `/api/inventory` | Create/update stock levels (admin). |

## Notifications (`notification-service`, direct port 8092)

| Method | Path | Description |
|---|---|---|
| GET | `/api/notifications` | List notifications. Supports `?recipientId=`. |

## Rate Limits (enforced at the gateway)

| Route | Requests/sec (per user) | Burst |
|---|---|---|
| `/api/products/**` | 10 | 20 |
| `/api/orders/**` | 5 | retries: 3 |
| `/api/payments/**` | 3 | 6 |

## Error Format

All services return errors in a consistent shape (see `common/common-lib`'s `ApiError`):

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Product not found: prod-999",
  "path": "/api/products/prod-999",
  "timestamp": "2026-07-22T10:15:30Z"
}
```
