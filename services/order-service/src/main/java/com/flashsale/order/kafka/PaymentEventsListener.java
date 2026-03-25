package com.flashsale.order.kafka;

import com.flashsale.order.domain.OrderEntity;
import com.flashsale.order.domain.OrderStatus;
import com.flashsale.order.events.PaymentResultEvent;
import com.flashsale.order.repo.OrderRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class PaymentEventsListener {

    private final OrderRepository orderRepo;

    public PaymentEventsListener(OrderRepository orderRepo) {
        this.orderRepo = orderRepo;
    }

    @KafkaListener(topics = "flashsale.payment.events", groupId = "order-service")
    @Transactional
    public void onPaymentResult(PaymentResultEvent event) {
        UUID orderId = UUID.fromString(event.orderId());
        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        // idempotent-ish
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CANCELLED) return;

        if ("SUCCEEDED".equalsIgnoreCase(event.status())) {
            order.setStatus(OrderStatus.PAID);
        } else {
            order.setStatus(OrderStatus.CANCELLED);
        }
        orderRepo.save(order);
    }
}