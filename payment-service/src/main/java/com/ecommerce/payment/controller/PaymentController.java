package com.ecommerce.payment.controller;

import com.ecommerce.payment.model.Payment;
import com.ecommerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment processing, transactions and refunds")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping
    @Operation(summary = "List all payments")
    public ResponseEntity<List<Payment>> list() {
        return ResponseEntity.ok(paymentService.findAll());
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get the payment for a given order")
    public ResponseEntity<Payment> getByOrder(@PathVariable String orderId) {
        return paymentService.findByOrderId(orderId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Process a payment for an order")
    public ResponseEntity<Payment> process(@RequestBody ProcessPaymentRequest request) {
        Payment payment = paymentService.process(
            request.orderId(), request.customerId(), request.amount(), request.paymentMethod());
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund a completed payment")
    public ResponseEntity<Payment> refund(@PathVariable String id, @RequestParam(defaultValue = "Order cancelled") String reason) {
        return ResponseEntity.ok(paymentService.refund(id, reason));
    }

    public record ProcessPaymentRequest(String orderId, String customerId, BigDecimal amount, String paymentMethod) {}
}
