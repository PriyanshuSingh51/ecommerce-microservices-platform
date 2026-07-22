package com.ecommerce.product.service;

import com.ecommerce.product.event.ProductEventProducer;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductEventProducer eventProducer;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> search(String term) {
        return productRepository.search(term);
    }

    public Optional<Product> findById(String id) {
        return productRepository.findById(id);
    }

    public Product create(Product product) {
        Product saved = productRepository.save(product);
        eventProducer.publishProductCreated(saved);
        return saved;
    }

    public Product update(String id, Product updated) {
        Product existing = productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        BigDecimal oldPrice = existing.getPrice();
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setPrice(updated.getPrice());
        existing.setCategory(updated.getCategory());
        existing.setStockQuantity(updated.getStockQuantity());

        Product saved = productRepository.save(existing);

        if (oldPrice.compareTo(updated.getPrice()) != 0) {
            eventProducer.publishPriceChanged(saved, oldPrice);
        }
        if (saved.getStockQuantity() == 0) {
            eventProducer.publishOutOfStock(saved);
        }
        return saved;
    }

    public void delete(String id) {
        productRepository.deleteById(id);
    }
}
