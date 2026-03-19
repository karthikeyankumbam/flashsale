package com.flashsale.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.order.api.dto.CreateOrderRequest;
import com.flashsale.order.domain.OrderEntity;
import com.flashsale.order.domain.OrderItemEntity;
import com.flashsale.order.repo.OrderRepository;
import com.flashsale.order.repo.OutboxRepository;
import com.flashsale.order.outbox.OutboxEventEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final OutboxRepository outboxRepo;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepo, OutboxRepository outboxRepo, ObjectMapper objectMapper) {
        this.orderRepo = orderRepo;
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderEntity createOrder(String userId, String idempotencyKey, CreateOrderRequest req) {
        // idempotency: return existing if already created
        var existing = orderRepo.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) return existing.get();

        OrderEntity order = new OrderEntity(userId, req.currency(), idempotencyKey);

        long total = 0;
        for (CreateOrderRequest.Item i : req.items()) {
            OrderItemEntity item = new OrderItemEntity(i.sku(), i.qty(), i.unitPrice());
            order.addItem(item);
            total += (long) i.qty() * i.unitPrice();
        }
        order.setTotalAmount(total);

        OrderEntity saved = orderRepo.save(order);

        // Outbox event (Kafka publish later)
        try {
            var payload = Map.of(
                    "orderId", saved.getId().toString(),
                    "userId", userId,
                    "total", total,
                    "currency", req.currency()
            );
            String json = objectMapper.writeValueAsString(payload);
            outboxRepo.save(new OutboxEventEntity("ORDER", saved.getId(), "OrderCreated", json));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }

        return saved;
    }

    public OrderEntity getOrder(String userId, java.util.UUID orderId) {
        // simple ownership check (since no JWT yet)
        OrderEntity o = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!o.getUserId().equals(userId)) throw new IllegalArgumentException("Order not found");
        return o;
    }
}