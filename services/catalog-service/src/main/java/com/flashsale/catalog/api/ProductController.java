package com.flashsale.catalog.api;

import com.flashsale.catalog.api.dto.CreateProductRequest;
import com.flashsale.catalog.api.dto.UpdateProductRequest;
import com.flashsale.catalog.product.Product;
import com.flashsale.catalog.product.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    // Public: list/search products
    @GetMapping
    public Page<Product> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "query") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);
        return service.search(category, query, page, size);
    }

    // Public: product details
    @GetMapping("/{sku}")
    public Product get(@PathVariable String sku) {
        return service.getBySku(sku);
    }

    // Admin: create
    @PostMapping
    public Product create(@Valid @RequestBody CreateProductRequest req) {
        return service.create(req);
    }

    // Admin: update
    @PutMapping("/{sku}")
    public Product update(@PathVariable String sku, @Valid @RequestBody UpdateProductRequest req) {
        return service.update(sku, req);
    }

    // Admin: delete
    @DeleteMapping("/{sku}")
    public void delete(@PathVariable String sku) {
        service.delete(sku);
    }
}