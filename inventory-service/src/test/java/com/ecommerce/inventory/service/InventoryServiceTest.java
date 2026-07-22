package com.ecommerce.inventory.service;

import com.ecommerce.inventory.event.InventoryEvents.OrderCreatedEvent;
import com.ecommerce.inventory.model.InventoryItem;
import com.ecommerce.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InventoryServiceTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        inventoryService = new InventoryService();
        var repoField = InventoryService.class.getDeclaredField("inventoryRepository");
        repoField.setAccessible(true);
        repoField.set(inventoryService, inventoryRepository);
        var kafkaField = InventoryService.class.getDeclaredField("kafkaTemplate");
        kafkaField.setAccessible(true);
        kafkaField.set(inventoryService, kafkaTemplate);
    }

    @Test
    void reserveForOrder_publishesUnavailableWhenStockInsufficient() {
        InventoryItem item = new InventoryItem("prod-1", 1);
        when(inventoryRepository.findById("prod-1")).thenReturn(Optional.of(item));

        OrderCreatedEvent event = new OrderCreatedEvent(
            "order-1", "cust-1", new BigDecimal("100.00"),
            List.of(new OrderCreatedEvent.Item("prod-1", 5, new BigDecimal("10.00"))),
            Instant.now()
        );

        inventoryService.reserveForOrder(event);

        verify(kafkaTemplate, times(1)).send(eq("inventory-events"), eq("order-1"), any());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void reserveForOrder_reservesStockWhenAvailable() {
        InventoryItem item = new InventoryItem("prod-1", 10);
        when(inventoryRepository.findById("prod-1")).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderCreatedEvent event = new OrderCreatedEvent(
            "order-1", "cust-1", new BigDecimal("20.00"),
            List.of(new OrderCreatedEvent.Item("prod-1", 2, new BigDecimal("10.00"))),
            Instant.now()
        );

        inventoryService.reserveForOrder(event);

        assertThat(item.getAvailableQuantity()).isEqualTo(8);
        assertThat(item.getReservedQuantity()).isEqualTo(2);
        verify(kafkaTemplate).send(eq("inventory-events"), eq("order-1"), any());
    }
}
