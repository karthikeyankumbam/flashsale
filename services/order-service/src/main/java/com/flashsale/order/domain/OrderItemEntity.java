package com.flashsale.order.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(nullable = false, length = 128)
    private String sku;

    @Column(nullable = false)
    private int qty;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    protected OrderItemEntity() {}

    public OrderItemEntity(String sku, int qty, long unitPrice) {
        this.sku = sku;
        this.qty = qty;
        this.unitPrice = unitPrice;
    }

    void setOrder(OrderEntity order) { this.order = order; }

    public String getSku() { return sku; }
    public int getQty() { return qty; }
    public long getUnitPrice() { return unitPrice; }
}