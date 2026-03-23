package com.flashsale.inventory.inventory;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "inventory")
public class InventoryEntity {

    @Id
    @Column(length = 128)
    private String sku;

    @Column(name = "available_qty", nullable = false)
    private int availableQty;

    @Column(name = "reserved_qty", nullable = false)
    private int reservedQty;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected InventoryEntity() {}

    public InventoryEntity(String sku, int availableQty) {
        this.sku = sku;
        this.availableQty = availableQty;
        this.reservedQty = 0;
    }

    @PreUpdate
    void preUpdate() { this.updatedAt = Instant.now(); }

    public String getSku() { return sku; }
    public int getAvailableQty() { return availableQty; }
    public int getReservedQty() { return reservedQty; }

    public void reserve(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");
        if (availableQty < qty) throw new IllegalArgumentException("Insufficient stock for sku=" + sku);
        availableQty -= qty;
        reservedQty += qty;
    }
}