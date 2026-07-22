package com.ecommerce.inventory.event;

import com.ecommerce.inventory.event.InventoryEvents.OrderCreatedEvent;
import com.ecommerce.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventListener {

    @Autowired
    private InventoryService inventoryService;

    @KafkaListener(topics = "order-events", groupId = "inventory-service-group")
    public void onOrderEvent(@Payload OrderCreatedEvent event) {
        inventoryService.reserveForOrder(event);
    }
}
