package com.flashsale.order.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.order.domain.OrderEntity;
import com.flashsale.order.domain.OrderStatus;
import com.flashsale.order.events.InventoryResultEvent;
import com.flashsale.order.events.PaymentRequestedEvent;
import com.flashsale.order.outbox.OutboxEventEntity;
import com.flashsale.order.repo.OrderRepository;
import com.flashsale.order.repo.OutboxRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Component
public class InventoryEventsListener {

    private final OrderRepository orderRepo;
    private final OutboxRepository outboxRepo;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryEventsListener(
            OrderRepository orderRepo,
            OutboxRepository outboxRepo,
            ObjectMapper objectMapper,
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.orderRepo = orderRepo;
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
            topics = "flashsale.inventory.events",
            groupId = "order-service",
            containerFactory = "inventoryResultKafkaListenerContainerFactory"
    )
    @Transactional
    public void onInventoryResult(InventoryResultEvent event) {
        UUID orderId = UUID.fromString(event.orderId());

        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        // If already terminal, ignore
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.PAID) {
            return;
        }

        if ("RESERVED".equalsIgnoreCase(event.status())) {
            // Move to payment pending
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            orderRepo.save(order);

            // Outbox for audit/debug (optional but good)
            saveOutbox(orderId, "InventoryReserved", Map.of(
                    "orderId", event.orderId(),
                    "reservationId", event.reservationId() == null ? "" : event.reservationId()
            ));

            // Publish payment command
            PaymentRequestedEvent payCmd = new PaymentRequestedEvent(
                    event.orderId(),
                    order.getTotalAmount(),
                    order.getCurrency()
            );
            kafkaTemplate.send("flashsale.payment.commands", event.orderId(), payCmd);

        } else if ("REJECTED".equalsIgnoreCase(event.status())) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepo.save(order);

            saveOutbox(orderId, "InventoryRejected", Map.of(
                    "orderId", event.orderId(),
                    "reason", event.reason() == null ? "" : event.reason()
            ));
        }
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