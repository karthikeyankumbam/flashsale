package com.flashsale.catalog.product;

import com.flashsale.catalog.api.dto.CreateProductRequest;
import com.flashsale.catalog.api.dto.UpdateProductRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ProductService {
    private final ProductRepository repo;
    private final ProductQueries queries;

    public ProductService(ProductRepository repo, ProductQueries queries) {
        this.repo = repo;
        this.queries = queries;
    }

    public Product create(CreateProductRequest req) {
        Product product = new Product(req.sku(), req.name().trim(), req.category().trim(),
                req.price(), req.currency().trim().toUpperCase(Locale.ROOT),
                req.active() == null || req.active(), req.attributes() == null ? Map.of() : req.attributes());
        product.setDescription(req.description() == null ? "" : req.description().trim());
        product.setImages(req.images() == null ? List.of() : List.copyOf(req.images()));
        try {
            // insert makes duplicate protection atomic, including concurrent requests.
            return repo.insert(product);
        } catch (DuplicateKeyException ex) {
            throw new DuplicateSkuException(req.sku());
        }
    }

    public Product getBySku(String sku) {
        Product product = getForAdmin(sku);
        if (!product.isActive()) throw new ProductNotFoundException(sku);
        return product;
    }

    public Product getForAdmin(String sku) {
        return repo.findById(sku).orElseThrow(() -> new ProductNotFoundException(sku));
    }

    public Page<Product> search(ProductSearch search, Boolean active) {
        return queries.search(search, active);
    }

    public List<String> categories() { return queries.categories(); }

    public Product update(String sku, UpdateProductRequest req) {
        Update changes = new Update().set("name", req.name().trim())
                .set("category", req.category().trim()).set("price", req.price())
                .set("currency", req.currency().trim().toUpperCase(Locale.ROOT))
                .set("updatedAt", Instant.now());
        // Omitted optional fields survive edits made by older clients.
        if (req.active() != null) changes.set("active", req.active());
        if (req.attributes() != null) changes.set("attributes", req.attributes());
        if (req.description() != null) changes.set("description", req.description().trim());
        if (req.images() != null) changes.set("images", req.images());
        return queries.update(sku, changes);
    }

    public Product setVisibility(String sku, boolean active) {
        return queries.update(sku, new Update().set("active", active).set("updatedAt", Instant.now()));
    }

    public void delete(String sku) {
        // Retain the SKU and product information for existing references and later republishing.
        setVisibility(sku, false);
    }
}
