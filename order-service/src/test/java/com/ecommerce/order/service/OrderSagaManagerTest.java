package com.ecommerce.order.service;

import com.ecommerce.order.controller.OrderController;
import com.ecommerce.order.event.OrderEventProducer;
import com.ecommerce.order.event.PaymentFailedEvent;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.saga.OrderSagaManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderSagaManagerTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderEventProducer eventProducer;
    private OrderSagaManager sagaManager;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        sagaManager = new OrderSagaManager();
        setField("orderRepository", orderRepository);
        setField("eventProducer", eventProducer);
    }

    private void setField(String name, Object value) throws Exception {
        var field = OrderSagaManager.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(sagaManager, value);
    }

    @Test
    void startNewOrder_createsOrderInPendingStateAndPublishesEvent() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId("order-1");
            return o;
        });

        List<OrderController.OrderItemRequest> items = List.of(
            new OrderController.OrderItemRequest("prod-1", 2, new BigDecimal("19.99"))
        );

        String orderId = sagaManager.startNewOrder("cust-1", items, new BigDecimal("39.98"));

        assertThat(orderId).isEqualTo("order-1");
        verify(eventProducer, times(1)).publish(eq("order-1"), any());
    }

    @Test
    void paymentFailed_cancelsOrderAndPublishesCancellation() {
        Order existing = new Order();
        existing.setId("order-1");
        existing.setCustomerId("cust-1");
        existing.setStatus(OrderStatus.INVENTORY_RESERVED);

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenReturn(existing);

        sagaManager.handlePaymentFailed(new PaymentFailedEvent("order-1", "Card declined"));

        assertThat(existing.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(eventProducer, times(1)).publish(eq("order-1"), any());
    }
}
