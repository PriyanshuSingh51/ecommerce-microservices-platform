package com.ecommerce.order.saga;

import com.ecommerce.order.event.*;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrates the order-processing Saga:
 *
 *   1. CREATE_ORDER        (this service)   -> emits OrderCreated
 *   2. RESERVE_INVENTORY   (inventory-svc)  -> emits InventoryReserved | InventoryUnavailable
 *   3. PROCESS_PAYMENT     (payment-svc)    -> emits PaymentCompleted | PaymentFailed
 *   4. CONFIRM_ORDER       (this service)   -> emits OrderConfirmed
 *
 * Any failure triggers compensating transactions (release inventory, refund
 * payment) and moves the order to CANCELLED.
 */
@Component
public class OrderSagaManager {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaManager.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderEventProducer eventProducer;

    @Transactional
    public String startNewOrder(String customerId,
                                 List<com.ecommerce.order.controller.OrderController.OrderItemRequest> requestItems,
                                 BigDecimal totalAmount) {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(totalAmount);
        order.setItems(requestItems.stream()
            .map(i -> new OrderItem(i.productId(), i.quantity(), i.price()))
            .collect(Collectors.toList()));

        Order saved = orderRepository.save(order);
        log.info("Starting order creation saga for order: {}", saved.getId());

        OrderCreatedEvent event = new OrderCreatedEvent(
            saved.getId(),
            saved.getCustomerId(),
            saved.getTotalAmount(),
            requestItems.stream().map(i -> new OrderCreatedEvent.Item(i.productId(), i.quantity(), i.price())).toList(),
            Instant.now()
        );
        eventProducer.publish(saved.getId(), event);
        log.info("Published OrderCreated event: {}", event);

        // Step 2 (Reserve inventory) is carried out asynchronously by
        // inventory-service, which consumes the OrderCreated event above.
        return saved.getId();
    }

    @Transactional
    public void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Inventory reserved for order: {}", event.orderId());
        Order order = orderRepository.findById(event.orderId())
            .orElseThrow(() -> new IllegalStateException("Order not found: " + event.orderId()));

        order.setStatus(OrderStatus.INVENTORY_RESERVED);
        orderRepository.save(order);

        // Step 3: payment-service consumes order-events/InventoryReserved
        // and initiates ProcessPaymentCommand on its own; nothing further
        // to orchestrate from here beyond the state transition above.
    }

    @Transactional
    public void handleInventoryUnavailable(InventoryUnavailableEvent event) {
        log.warn("Inventory unavailable for order: {}, reason: {}", event.orderId(), event.reason());
        cancelOrder(event.orderId(), "Inventory unavailable: " + event.reason());
    }

    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Payment completed for order: {}", event.orderId());
        Order order = orderRepository.findById(event.orderId())
            .orElseThrow(() -> new IllegalStateException("Order not found: " + event.orderId()));

        order.setStatus(OrderStatus.PAYMENT_COMPLETED);
        order.setPaymentId(event.paymentId());
        orderRepository.save(order);

        // Step 4: confirm order
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(Instant.now());
        orderRepository.save(order);

        eventProducer.publish(order.getId(), new OrderConfirmedEvent(order.getId(), order.getCustomerId(), order.getConfirmedAt()));
        log.info("Order {} confirmed successfully", order.getId());
    }

    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.warn("Payment failed for order: {}, reason: {}", event.orderId(), event.failureReason());
        cancelOrder(event.orderId(), "Payment failed: " + event.failureReason());
    }

    @Transactional
    public void cancelOrder(String orderId, String reason) {
        log.info("Cancelling order: {}, reason: {}", orderId, reason);
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        order.setCancelledAt(Instant.now());
        orderRepository.save(order);

        // Compensating transactions: inventory-service and payment-service
        // each listen for OrderCancelled and release/refund accordingly.
        eventProducer.publish(orderId, new OrderCancelledEvent(orderId, order.getCustomerId(), reason, order.getCancelledAt()));
        log.info("Order {} cancelled successfully", orderId);
    }
}
