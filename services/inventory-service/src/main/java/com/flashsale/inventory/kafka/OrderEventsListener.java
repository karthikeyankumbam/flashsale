package com.flashsale.inventory.kafka;

import com.flashsale.inventory.events.InventoryResultEvent;
import com.flashsale.inventory.events.OrderCreatedEvent;
import com.flashsale.inventory.service.InventoryReservationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventsListener {

    private final InventoryReservationService reservationService;
    private final KafkaTemplate<String, InventoryResultEvent> kafkaTemplate;

    public OrderEventsListener(InventoryReservationService reservationService,
                               KafkaTemplate<String, InventoryResultEvent> kafkaTemplate) {
        this.reservationService = reservationService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "flashsale.order.events", groupId = "inventory-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        String orderId = event.orderId();
        try {
            var res = reservationService.reserve(event);
            kafkaTemplate.send("flashsale.inventory.events", orderId,
                    new InventoryResultEvent(orderId, "RESERVED", null, res.getOrderId().toString()));
        } catch (Exception ex) {
            reservationService.reject(event, ex.getMessage());
            kafkaTemplate.send("flashsale.inventory.events", orderId,
                    new InventoryResultEvent(orderId, "REJECTED", ex.getMessage(), null));
        }
    }
}