package com.ecommerce.order.event;

import java.time.Instant;

public record OrderConfirmedEvent(String orderId, String customerId, Instant confirmedAt) {}
