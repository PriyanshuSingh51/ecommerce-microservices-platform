package com.ecommerce.payment.event;

import com.ecommerce.payment.event.PaymentEvents.OrderCreatedEvent;
import com.ecommerce.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * In the full saga, payment-service actually reacts to InventoryReserved
 * (once stock is confirmed available), not directly to OrderCreated. This
 * listener is kept simple for illustration; wire it to the appropriate
 * topic/event type for your business rules.
 */
@Component
public class PaymentEventListener {

    @Autowired
    private PaymentService paymentService;

    @KafkaListener(topics = "inventory-events", groupId = "payment-service-group")
    public void onInventoryReserved(@Payload Object event) {
        // Expect a map/DTO carrying orderId, customerId, totalAmount, paymentMethod
        // once inventory confirms stock; process payment accordingly.
    }
}
