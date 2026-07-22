package com.ecommerce.inventory.service;

import com.ecommerce.inventory.event.InventoryEvents.*;
import com.ecommerce.inventory.model.InventoryItem;
import com.ecommerce.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final String TOPIC = "inventory-events";
    private static final int LOW_STOCK_THRESHOLD = 10;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public List<InventoryItem> findAll() { return inventoryRepository.findAll(); }

    public Optional<InventoryItem> findByProductId(String productId) { return inventoryRepository.findById(productId); }

    /**
     * Attempts to reserve stock for every line item of an order. If any
     * item is unavailable, all reservations made so far in this call are
     * rolled back (the @Transactional boundary) and an
     * InventoryUnavailableEvent is published so the saga can compensate.
     */
    @Transactional
    public void reserveForOrder(OrderCreatedEvent event) {
        for (OrderCreatedEvent.Item item : event.items()) {
            InventoryItem inventory = inventoryRepository.findById(item.productId())
                .orElseThrow(() -> new IllegalStateException("Unknown product: " + item.productId()));

            if (inventory.getAvailableQuantity() < item.quantity()) {
                log.warn("Insufficient stock for product {} on order {}", item.productId(), event.orderId());
                kafkaTemplate.send(TOPIC, event.orderId(),
                    new InventoryUnavailableEvent(event.orderId(), "Insufficient stock for product " + item.productId()));
                return;
            }
        }

        for (OrderCreatedEvent.Item item : event.items()) {
            InventoryItem inventory = inventoryRepository.findById(item.productId()).orElseThrow();
            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - item.quantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() + item.quantity());
            inventoryRepository.save(inventory);

            kafkaTemplate.send(TOPIC, item.productId(), new StockReservedEvent(item.productId(), item.quantity()));
            if (inventory.getAvailableQuantity() <= LOW_STOCK_THRESHOLD) {
                kafkaTemplate.send(TOPIC, item.productId(), new LowStockWarningEvent(item.productId(), inventory.getAvailableQuantity()));
            }
        }

        log.info("Inventory reserved for order {}", event.orderId());
        kafkaTemplate.send(TOPIC, event.orderId(), new InventoryReservedEvent(event.orderId(), "default"));
    }

    /** Compensating transaction: releases previously reserved stock when an order is cancelled. */
    @Transactional
    public void releaseForOrder(OrderCancelledEvent event, List<OrderCreatedEvent.Item> items) {
        for (OrderCreatedEvent.Item item : items) {
            inventoryRepository.findById(item.productId()).ifPresent(inv -> {
                inv.setAvailableQuantity(inv.getAvailableQuantity() + item.quantity());
                inv.setReservedQuantity(Math.max(0, inv.getReservedQuantity() - item.quantity()));
                inventoryRepository.save(inv);
                kafkaTemplate.send(TOPIC, item.productId(), new StockReleasedEvent(item.productId(), item.quantity()));
            });
        }
        log.info("Released reserved inventory for cancelled order {}", event.orderId());
    }
}
