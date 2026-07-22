package com.ecommerce.order.controller;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.saga.OrderSagaManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order processing & saga orchestration")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderSagaManager sagaManager;

    @GetMapping
    @Operation(summary = "List orders, optionally by customer")
    public ResponseEntity<List<Order>> list(@RequestParam(required = false) String customerId) {
        if (customerId != null) return ResponseEntity.ok(orderRepository.findByCustomerId(customerId));
        return ResponseEntity.ok(orderRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order status and details")
    public ResponseEntity<Order> getById(@PathVariable String id) {
        return orderRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create an order; kicks off the order-processing saga")
    public ResponseEntity<Map<String, String>> create(@RequestBody CreateOrderRequest request) {
        String orderId = sagaManager.startNewOrder(request.customerId(), request.items(), request.totalAmount());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
            "orderId", orderId,
            "status", "PENDING",
            "message", "Order accepted; saga in progress"
        ));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order and trigger compensating transactions")
    public ResponseEntity<Void> cancel(@PathVariable String id, @RequestParam(defaultValue = "Customer requested cancellation") String reason) {
        sagaManager.cancelOrder(id, reason);
        return ResponseEntity.accepted().build();
    }

    public record CreateOrderRequest(String customerId, java.util.List<OrderItemRequest> items, java.math.BigDecimal totalAmount) {}
    public record OrderItemRequest(String productId, int quantity, java.math.BigDecimal price) {}
}
