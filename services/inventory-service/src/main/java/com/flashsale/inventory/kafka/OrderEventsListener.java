package com.flashsale.inventory.kafka;

import com.flashsale.inventory.events.*;
import com.flashsale.inventory.service.InventoryReservationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OrderEventsListener {

    private final InventoryReservationService reservationService;
    private final KafkaTemplate<String, InventoryResultEvent> kafkaTemplate;
    private final KafkaTemplate<String, Object> dlqTemplate;

    public OrderEventsListener(
            InventoryReservationService reservationService,
            KafkaTemplate<String, InventoryResultEvent> kafkaTemplate,
            KafkaTemplate<String, Object> dlqTemplate
    ) {
        this.reservationService = reservationService;
        this.kafkaTemplate = kafkaTemplate;
        this.dlqTemplate = dlqTemplate;
    }

    @KafkaListener(topics = "flashsale.order.events", groupId = "inventory-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        String orderId = event.orderId();
        try {
            var res = reservationService.reserve(event);

            kafkaTemplate.send("flashsale.inventory.events", orderId,
                    new InventoryResultEvent(orderId, "RESERVED", null, res.getOrderId().toString()));

        } catch (Exception ex) {
            // publish result as REJECTED (so order-service can cancel)
            kafkaTemplate.send("flashsale.inventory.events", orderId,
                    new InventoryResultEvent(orderId, "REJECTED", ex.getMessage(), null));

            // also publish to DLQ for inspection if it's a poison message
            dlqTemplate.send("flashsale.deadletter", orderId,
                    new DeadLetterEvent(
                            "flashsale.order.events",
                            "inventory-service",
                            orderId,
                            ex.getMessage(),
                            safeToString(event),
                            Instant.now()
                    ));
        }
    }

    private String safeToString(Object o) {
        try { return String.valueOf(o); }
        catch (Exception e) { return "<unprintable>"; }
    }
}