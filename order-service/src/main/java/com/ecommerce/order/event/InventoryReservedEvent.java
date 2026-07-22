package com.ecommerce.order.event;

public record InventoryReservedEvent(String orderId, String paymentMethod) {}
