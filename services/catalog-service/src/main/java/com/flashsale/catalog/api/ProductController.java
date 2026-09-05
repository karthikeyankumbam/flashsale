package com.flashsale.catalog.api;

import com.flashsale.catalog.api.dto.*;
import com.flashsale.catalog.product.*;
import com.flashsale.catalog.validation.CurrencyCode;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Products")
@Validated
@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) { this.service = service; }

    @Operation(summary = "Search and browse published products")
    @GetMapping
    public ProductPage list(
            @RequestParam(required = false) @Size(max = 100) String category,
            @RequestParam(required = false) @Size(max = 200) String query,
            @RequestParam(required = false) @CurrencyCode String currency,
            @RequestParam(required = false) @PositiveOrZero Long minPrice,
            @RequestParam(required = false) @PositiveOrZero Long maxPrice,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "name-asc") String sort) {
        return ProductPage.from(service.search(
                new ProductSearch(category, query, currency, minPrice, maxPrice, page, size, sort), true));
    }

    // Two segments avoid reserving an existing product's SKU such as "categories".
    @Operation(summary = "List categories containing published products")
    @GetMapping("/browse/categories")
    public List<String> categories() { return service.categories(); }

    @Operation(summary = "View a published product", description = "Hidden and missing products return 404.")
    @GetMapping("/{sku}")
    public Product get(@PathVariable String sku) { return service.getBySku(sku); }

    // Existing write URLs remain available, with ADMIN authorization.
    @Operation(summary = "Create a product (legacy URL)", description = "Requires ADMIN. The admin/items endpoint provides the same operation.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody CreateProductRequest req) { return service.create(req); }

    @Operation(summary = "Edit a product (legacy URL)", description = "Requires ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{sku}")
    public Product update(@PathVariable String sku, @Valid @RequestBody UpdateProductRequest req) {
        return service.update(sku, req);
    }

    @Operation(summary = "Hide a product (legacy URL)", description = "Requires ADMIN. Does not permanently delete the record.")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{sku}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String sku) { service.delete(sku); }
}
