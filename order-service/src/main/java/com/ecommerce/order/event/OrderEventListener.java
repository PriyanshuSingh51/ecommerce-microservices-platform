package com.ecommerce.order.event;

import com.ecommerce.order.saga.OrderSagaManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Listens for events published by inventory-service and payment-service
 * and drives the order saga forward (or triggers compensation) accordingly.
 */
@Component
public class OrderEventListener {

    @Autowired
    private OrderSagaManager sagaManager;

    @KafkaListener(topics = "inventory-events", groupId = "order-service-group")
    public void onInventoryEvent(@Payload Object event, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        if (event instanceof InventoryReservedEvent reserved) {
            sagaManager.handleInventoryReserved(reserved);
        } else if (event instanceof InventoryUnavailableEvent unavailable) {
            sagaManager.handleInventoryUnavailable(unavailable);
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "order-service-group")
    public void onPaymentEvent(@Payload Object event, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        if (event instanceof PaymentCompletedEvent completed) {
            sagaManager.handlePaymentCompleted(completed);
        } else if (event instanceof PaymentFailedEvent failed) {
            sagaManager.handlePaymentFailed(failed);
        }
    }
}
