package com.ecommerce.product.service;

import com.ecommerce.product.event.ProductEventProducer;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductEventProducer eventProducer;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productService = new ProductService();
        // field injection via reflection is avoided in real code by using
        // constructor injection; kept simple here for illustration.
        setField("productRepository", productRepository);
        setField("eventProducer", eventProducer);
    }

    private void setField(String name, Object value) {
        try {
            var field = ProductService.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(productService, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createProduct_savesAndPublishesEvent() {
        Product product = new Product("Wireless Mouse", "Ergonomic mouse", new BigDecimal("29.99"), "Electronics", 100);
        product.setId("p-1");
        when(productRepository.save(product)).thenReturn(product);

        Product result = productService.create(product);

        assertThat(result.getId()).isEqualTo("p-1");
        verify(eventProducer, times(1)).publishProductCreated(product);
    }

    @Test
    void findById_returnsEmptyWhenMissing() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        Optional<Product> result = productService.findById("missing");

        assertThat(result).isEmpty();
    }

    @Test
    void update_publishesPriceChangedEventWhenPriceDiffers() {
        Product existing = new Product("Mouse", "desc", new BigDecimal("10.00"), "Electronics", 5);
        existing.setId("p-1");
        Product updated = new Product("Mouse", "desc", new BigDecimal("15.00"), "Electronics", 5);

        when(productRepository.findById("p-1")).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenReturn(existing);

        productService.update("p-1", updated);

        verify(eventProducer, times(1)).publishPriceChanged(any(Product.class), eq(new BigDecimal("10.00")));
    }
}
