package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.model.InventoryItem;
import com.ecommerce.inventory.repository.InventoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Stock management and reservations")
public class InventoryController {

    @Autowired
    private InventoryRepository inventoryRepository;

    @GetMapping
    @Operation(summary = "List all inventory records")
    public ResponseEntity<List<InventoryItem>> list() {
        return ResponseEntity.ok(inventoryRepository.findAll());
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory for a product")
    public ResponseEntity<InventoryItem> getByProduct(@PathVariable String productId) {
        return inventoryRepository.findById(productId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create or update stock levels for a product (admin)")
    public ResponseEntity<InventoryItem> upsert(@RequestBody InventoryItem item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryRepository.save(item));
    }
}
