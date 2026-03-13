package com.flashsale.catalog.product;

import com.flashsale.catalog.api.dto.CreateProductRequest;
import com.flashsale.catalog.api.dto.UpdateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    public Product create(CreateProductRequest req) {
        if (repo.existsById(req.sku())) {
            throw new IllegalArgumentException("SKU already exists: " + req.sku());
        }
        Product p = new Product(
                req.sku(),
                req.name(),
                req.category(),
                req.price(),
                req.currency(),
                req.active() == null || req.active(),
                req.attributes()
        );
        return repo.save(p);
    }

    public Product getBySku(String sku) {
        return repo.findById(sku).orElseThrow(() -> new IllegalArgumentException("Product not found: " + sku));
    }

    public Page<Product> search(String category, String query, int page, int size) {
        var pageable = PageRequest.of(page, size);

        boolean hasCategory = category != null && !category.isBlank();
        boolean hasQuery = query != null && !query.isBlank();

        if (hasCategory && hasQuery) {
            return repo.findByActiveTrueAndCategoryIgnoreCaseAndNameContainingIgnoreCase(category, query, pageable);
        }
        if (hasCategory) {
            return repo.findByActiveTrueAndCategoryIgnoreCase(category, pageable);
        }
        if (hasQuery) {
            return repo.findByActiveTrueAndNameContainingIgnoreCase(query, pageable);
        }
        return repo.findByActiveTrue(pageable);
    }

    public Product update(String sku, UpdateProductRequest req) {
        Product p = getBySku(sku);

        p.setName(req.name());
        p.setCategory(req.category());
        p.setPrice(req.price());
        p.setCurrency(req.currency());
        if (req.active() != null) p.setActive(req.active());
        p.setAttributes(req.attributes());

        p.touch();
        return repo.save(p);
    }

    public void delete(String sku) {
        if (!repo.existsById(sku)) {
            throw new IllegalArgumentException("Product not found: " + sku);
        }
        repo.deleteById(sku);
    }
}