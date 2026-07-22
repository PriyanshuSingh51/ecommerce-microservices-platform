package com.ecommerce.payment.service;

import com.ecommerce.payment.event.PaymentEvents.*;
import com.ecommerce.payment.model.Payment;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String TOPIC = "payment-events";
    private final Random random = new Random();

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public List<Payment> findAll() { return paymentRepository.findAll(); }

    public Optional<Payment> findByOrderId(String orderId) { return paymentRepository.findByOrderId(orderId); }

    /**
     * Simulates processing a payment through an upstream gateway (Stripe/PayPal/etc.)
     * In production this method calls out to the real gateway client instead of
     * randomly succeeding/failing.
     */
    public Payment process(String orderId, String customerId, BigDecimal amount, String method) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setCustomerId(customerId);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);

        boolean success = random.nextDouble() > 0.05; // 95% success rate simulation
        payment.setStatus(success ? PaymentStatus.COMPLETED : PaymentStatus.FAILED);
        payment.setProcessedAt(Instant.now());
        if (!success) payment.setFailureReason("Payment gateway declined the transaction");

        Payment saved = paymentRepository.save(payment);

        if (success) {
            kafkaTemplate.send(TOPIC, orderId, new PaymentCompletedEvent(orderId, saved.getId()));
            log.info("Payment {} completed for order {}", saved.getId(), orderId);
        } else {
            kafkaTemplate.send(TOPIC, orderId, new PaymentFailedEvent(orderId, saved.getFailureReason()));
            log.warn("Payment failed for order {}: {}", orderId, saved.getFailureReason());
        }
        return saved;
    }

    public Payment refund(String paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        payment.setStatus(PaymentStatus.REFUNDED);
        Payment saved = paymentRepository.save(payment);
        kafkaTemplate.send(TOPIC, payment.getOrderId(), new RefundProcessedEvent(payment.getOrderId(), payment.getId()));
        log.info("Refund processed for payment {} (order {}), reason: {}", paymentId, payment.getOrderId(), reason);
        return saved;
    }
}
