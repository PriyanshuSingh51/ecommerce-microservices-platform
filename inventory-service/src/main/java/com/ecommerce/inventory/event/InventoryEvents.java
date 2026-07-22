package com.ecommerce.inventory.event;

public class InventoryEvents {
    public record OrderCreatedEvent(String orderId, String customerId, java.math.BigDecimal totalAmount,
                                     java.util.List<Item> items, java.time.Instant timestamp) {
        public record Item(String productId, int quantity, java.math.BigDecimal price) {}
    }
    public record OrderCancelledEvent(String orderId, String customerId, String reason, java.time.Instant cancelledAt) {}
    public record InventoryReservedEvent(String orderId, String paymentMethod) {}
    public record InventoryUnavailableEvent(String orderId, String reason) {}
    public record StockReservedEvent(String productId, int quantity) {}
    public record StockReleasedEvent(String productId, int quantity) {}
    public record LowStockWarningEvent(String productId, int remaining) {}
}
