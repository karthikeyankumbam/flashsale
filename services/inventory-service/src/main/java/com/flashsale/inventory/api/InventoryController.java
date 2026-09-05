package com.flashsale.inventory.api;

import com.flashsale.inventory.inventory.InventoryEntity;
import com.flashsale.inventory.inventory.InventoryRepository;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Inventory")
@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryRepository repo;

    public InventoryController(InventoryRepository repo) {
        this.repo = repo;
    }

    @Operation(summary = "Check available and reserved stock")
    @GetMapping("/{sku}")
    public InventoryEntity get(@Parameter(example = "IPHONE-16-128-BLK") @PathVariable String sku) {
        return repo.findById(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));
    }
}
