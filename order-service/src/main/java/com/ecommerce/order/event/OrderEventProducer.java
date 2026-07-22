package com.ecommerce.order.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    private static final String ORDER_EVENTS_TOPIC = "order-events";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String key, Object event) {
        kafkaTemplate.send(ORDER_EVENTS_TOPIC, key, event);
    }
}
