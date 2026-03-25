package com.flashsale.inventory.api;

import com.flashsale.inventory.inventory.InventoryEntity;
import com.flashsale.inventory.inventory.InventoryRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryRepository repo;

    public InventoryController(InventoryRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/{sku}")
    public InventoryEntity get(@PathVariable String sku) {
        return repo.findById(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));
    }
}