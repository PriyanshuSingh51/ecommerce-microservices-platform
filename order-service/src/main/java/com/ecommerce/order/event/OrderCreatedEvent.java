package com.ecommerce.order.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderCreatedEvent(
    String orderId,
    String customerId,
    BigDecimal totalAmount,
    List<Item> items,
    Instant timestamp
) {
    public record Item(String productId, int quantity, BigDecimal price) {}
}
