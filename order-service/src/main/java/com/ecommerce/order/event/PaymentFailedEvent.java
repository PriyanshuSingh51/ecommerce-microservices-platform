package com.ecommerce.order.event;

public record PaymentFailedEvent(String orderId, String failureReason) {}
