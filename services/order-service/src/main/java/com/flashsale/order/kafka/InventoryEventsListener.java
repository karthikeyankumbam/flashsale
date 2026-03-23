package com.flashsale.order.kafka;

import com.flashsale.order.domain.OrderEntity;
import com.flashsale.order.domain.OrderStatus;
import com.flashsale.order.events.InventoryResultEvent;
import com.flashsale.order.outbox.OutboxEventEntity;
import com.flashsale.order.repo.OrderRepository;
import com.flashsale.order.repo.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Component
public class InventoryEventsListener {

    private final OrderRepository orderRepo;
    private final OutboxRepository outboxRepo;
    private final ObjectMapper objectMapper;

    public InventoryEventsListener(OrderRepository orderRepo, OutboxRepository outboxRepo, ObjectMapper objectMapper) {
        this.orderRepo = orderRepo;
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "flashsale.inventory.events", groupId = "order-service")
    @Transactional
    public void onInventoryResult(InventoryResultEvent event) {
        UUID orderId = UUID.fromString(event.orderId());

        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        // idempotent-ish: if already cancelled/confirmed, ignore
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.CONFIRMED) {
            return;
        }

        if ("RESERVED".equalsIgnoreCase(event.status())) {
            order.setStatus(OrderStatus.CONFIRMED);

            saveOutbox(orderId, "InventoryReserved", Map.of(
                    "orderId", event.orderId(),
                    "reservationId", event.reservationId() == null ? "" : event.reservationId()
            ));
        } else if ("REJECTED".equalsIgnoreCase(event.status())) {
            order.setStatus(OrderStatus.CANCELLED);

            saveOutbox(orderId, "InventoryRejected", Map.of(
                    "orderId", event.orderId(),
                    "reason", event.reason() == null ? "" : event.reason()
            ));
        }
        // JPA will flush on commit
        orderRepo.save(order);
    }

    private void saveOutbox(UUID orderId, String eventType, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxRepo.save(new OutboxEventEntity("ORDER", orderId, eventType, json));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}