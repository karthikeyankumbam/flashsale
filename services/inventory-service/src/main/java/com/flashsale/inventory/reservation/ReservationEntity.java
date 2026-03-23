package com.flashsale.inventory.reservation;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class ReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(nullable = false, length = 16)
    private String status; // RESERVED / REJECTED

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ReservationEntity() {}

    public ReservationEntity(UUID orderId, String status) {
        this.orderId = orderId;
        this.status = status;
    }

    public UUID getOrderId() { return orderId; }
    public String getStatus() { return status; }
}