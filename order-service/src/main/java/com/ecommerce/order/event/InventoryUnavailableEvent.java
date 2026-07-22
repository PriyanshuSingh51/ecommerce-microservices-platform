package com.ecommerce.order.event;

public record InventoryUnavailableEvent(String orderId, String reason) {}
