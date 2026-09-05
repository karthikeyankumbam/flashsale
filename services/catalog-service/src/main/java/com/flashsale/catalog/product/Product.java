package com.flashsale.catalog.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "products")
public class Product {

    @Id
    private String sku; // use SKU as Mongo _id

    @Indexed
    @NotBlank
    private String name;

    @Indexed
    @NotBlank
    private String category;

    @NotNull
    private Long price; // Existing whole-currency-unit convention; do not rescale stored values.

    @NotBlank
    private String currency; // "INR"

    private boolean active = true;

    private String description = "";
    private List<String> images = List.of();

    // flexible product attributes: { "color":"Black", "storage":"128GB" }
    private Map<String, Object> attributes;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Product() {}

    public Product(String sku, String name, String category, Long price, String currency, boolean active, Map<String, Object> attributes) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.price = price;
        this.currency = currency;
        this.active = active;
        this.attributes = attributes;
    }

    public void touch() { this.updatedAt = Instant.now(); }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getDescription() { return description == null ? "" : description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getImages() { return images == null ? List.of() : images; }
    public void setImages(List<String> images) { this.images = images; }

    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
