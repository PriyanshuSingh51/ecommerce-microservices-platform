package com.ecommerce.payment.service;

import com.ecommerce.payment.model.Payment;
import com.ecommerce.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        paymentService = new PaymentService();
        setField("paymentRepository", paymentRepository);
        setField("kafkaTemplate", kafkaTemplate);
    }

    private void setField(String name, Object value) throws Exception {
        var field = PaymentService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(paymentService, value);
    }

    @Test
    void process_savesPaymentWithAmountAndOrderId() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.process("order-1", "cust-1", new BigDecimal("50.00"), "CREDIT_CARD");

        assertThat(result.getOrderId()).isEqualTo("order-1");
        assertThat(result.getAmount()).isEqualByComparingTo("50.00");
    }
}
