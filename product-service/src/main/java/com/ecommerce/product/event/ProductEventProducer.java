package com.ecommerce.product.event;

import com.ecommerce.product.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class ProductEventProducer {

    private static final String TOPIC = "product-events";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publishProductCreated(Product product) {
        kafkaTemplate.send(TOPIC, product.getId(), Map.of(
            "eventType", "ProductCreated",
            "productId", product.getId(),
            "name", product.getName(),
            "price", product.getPrice(),
            "timestamp", Instant.now().toString()
        ));
    }

    public void publishPriceChanged(Product product, java.math.BigDecimal oldPrice) {
        kafkaTemplate.send(TOPIC, product.getId(), Map.of(
            "eventType", "ProductPriceChanged",
            "productId", product.getId(),
            "oldPrice", oldPrice,
            "newPrice", product.getPrice(),
            "timestamp", Instant.now().toString()
        ));
    }

    public void publishOutOfStock(Product product) {
        kafkaTemplate.send(TOPIC, product.getId(), Map.of(
            "eventType", "ProductOutOfStock",
            "productId", product.getId(),
            "timestamp", Instant.now().toString()
        ));
    }
}
