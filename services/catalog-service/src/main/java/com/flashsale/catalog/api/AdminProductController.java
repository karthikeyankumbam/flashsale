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

@Tag(name = "Catalog administration", description = "Requires the ADMIN role.")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/products/admin/items")
public class AdminProductController {
    private final ProductService service;

    public AdminProductController(ProductService service) { this.service = service; }

    @Operation(summary = "Find published and hidden products")
    @GetMapping
    public ProductPage list(
            @RequestParam(required = false) @Size(max = 100) String category,
            @RequestParam(required = false) @Size(max = 200) String query,
            @RequestParam(required = false) @CurrencyCode String currency,
            @RequestParam(required = false) @PositiveOrZero Long minPrice,
            @RequestParam(required = false) @PositiveOrZero Long maxPrice,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "updated") String sort,
            @RequestParam(defaultValue = "all") String visibility) {
        Boolean active = switch (visibility) {
            case "all" -> null;
            case "published" -> true;
            case "hidden" -> false;
            default -> throw new IllegalArgumentException("visibility must be all, published or hidden");
        };
        return ProductPage.from(service.search(
                new ProductSearch(category, query, currency, minPrice, maxPrice, page, size, sort), active));
    }

    @Operation(summary = "Preview a product, including hidden products")
    @GetMapping("/{sku}")
    public Product get(@PathVariable String sku) { return service.getForAdmin(sku); }

    @Operation(summary = "Create a product", description = "Set active=false to create it hidden. SKU must be unique.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody CreateProductRequest req) { return service.create(req); }

    @Operation(summary = "Edit product details", description = "Optional fields omitted from the request retain their existing values.")
    @PutMapping("/{sku}")
    public Product update(@PathVariable String sku, @Valid @RequestBody UpdateProductRequest req) {
        return service.update(sku, req);
    }

    @Operation(summary = "Publish or hide a product")
    @PutMapping("/{sku}/visibility")
    public Product visibility(@PathVariable String sku, @Valid @RequestBody VisibilityRequest req) {
        return service.setVisibility(sku, req.active());
    }

    @Operation(summary = "Hide a product", description = "Preserves its record. It can be published again later.")
    @DeleteMapping("/{sku}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hide(@PathVariable String sku) { service.delete(sku); }
}
