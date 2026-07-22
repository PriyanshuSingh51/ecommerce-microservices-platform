package com.ecommerce.payment.event;

public class PaymentEvents {
    public record OrderCreatedEvent(String orderId, String customerId, java.math.BigDecimal totalAmount) {}
    public record PaymentCompletedEvent(String orderId, String paymentId) {}
    public record PaymentFailedEvent(String orderId, String failureReason) {}
    public record RefundProcessedEvent(String orderId, String paymentId) {}
}
