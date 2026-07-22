package com.ecommerce.order.event;

public record PaymentCompletedEvent(String orderId, String paymentId) {}
