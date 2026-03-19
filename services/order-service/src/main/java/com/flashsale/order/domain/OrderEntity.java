package com.flashsale.order.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders",
        uniqueConstraints = @UniqueConstraint(name = "uq_orders_user_idem", columnNames = {"user_id", "idempotency_key"}))
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();

    protected OrderEntity() {}

    public OrderEntity(String userId, String currency, String idempotencyKey) {
        this.userId = userId;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.status = OrderStatus.PENDING;
    }

    @PreUpdate
    void preUpdate() { this.updatedAt = Instant.now(); }

    public void addItem(OrderItemEntity item) {
        item.setOrder(this);
        this.items.add(item);
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(long totalAmount) { this.totalAmount = totalAmount; }
    public String getCurrency() { return currency; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public List<OrderItemEntity> getItems() { return items; }
}