package com.flashsale.order.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "payload_json", nullable = false, columnDefinition = "text")
    private String payloadJson;

    @Column(nullable = false, length = 16)
    private String status = "NEW"; // NEW, SENT, FAILED

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected OutboxEventEntity() {}

    public OutboxEventEntity(String aggregateType, UUID aggregateId, String eventType, String payloadJson) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payloadJson = payloadJson;
    }

    public UUID getId() { return id; }
    public String getEventType() { return eventType; }
    public String getPayloadJson() { return payloadJson; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void markSent() {
        this.status = "SENT";
        this.sentAt = Instant.now();
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.status = "FAILED";
        this.lastError = error;
    }

    public void incrementAttempt(String error) {
        this.attemptCount++;
        this.lastError = error;
    }
}