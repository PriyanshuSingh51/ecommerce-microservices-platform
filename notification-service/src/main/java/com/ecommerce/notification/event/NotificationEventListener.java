package com.ecommerce.notification.event;

import com.ecommerce.notification.model.NotificationChannel;
import com.ecommerce.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes events from every domain topic and turns the interesting ones
 * into customer-facing notifications (order confirmations, shipping
 * updates, payment receipts, etc).
 */
@Component
public class NotificationEventListener {

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "order-events", groupId = "notification-service-group")
    public void onOrderEvent(@Payload Map<String, Object> event) {
        Object eventType = event.get("eventType");
        Object orderId = event.get("orderId");
        Object customerId = event.get("customerId");
        if (customerId == null) return;

        if ("OrderConfirmed".equals(eventType)) {
            notificationService.send(customerId.toString(), NotificationChannel.EMAIL,
                "Your order is confirmed!", "Order " + orderId + " has been confirmed and is being prepared.", "OrderConfirmed");
        } else if ("OrderCancelled".equals(eventType)) {
            notificationService.send(customerId.toString(), NotificationChannel.EMAIL,
                "Your order was cancelled", "Order " + orderId + " has been cancelled: " + event.get("reason"), "OrderCancelled");
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-service-group")
    public void onPaymentEvent(@Payload Map<String, Object> event) {
        // e.g. send a payment receipt or a "payment failed, please retry" notice
    }
}
