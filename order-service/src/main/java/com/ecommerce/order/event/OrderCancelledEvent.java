package com.ecommerce.order.event;

import java.time.Instant;

public record OrderCancelledEvent(String orderId, String customerId, String reason, Instant cancelledAt) {}
