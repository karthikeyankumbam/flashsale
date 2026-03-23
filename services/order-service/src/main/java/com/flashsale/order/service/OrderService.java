package com.flashsale.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.order.api.dto.CreateOrderRequest;
import com.flashsale.order.domain.OrderEntity;
import com.flashsale.order.domain.OrderItemEntity;
import com.flashsale.order.domain.OrderStatus;
import com.flashsale.order.outbox.OutboxEventEntity;
import com.flashsale.order.repo.OrderRepository;
import com.flashsale.order.repo.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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

        // outbox: OrderCreated
        saveOutbox(saved.getId(), "OrderCreated", Map.of(
                "orderId", saved.getId().toString(),
                "userId", userId,
                "total", total,
                "currency", req.currency()
        ));

        return saved;
    }

    @Transactional(readOnly = true)
    public OrderEntity getOrder(String userId, UUID orderId) {
        // simple ownership check (since no JWT yet)
        OrderEntity o = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!o.getUserId().equals(userId)) throw new IllegalArgumentException("Order not found");
        return o;
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> listOrders(String userId) {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public OrderEntity cancelOrder(String userId, UUID orderId, String reason) {
        OrderEntity o = getOrder(userId, orderId);

        if (o.getStatus() == OrderStatus.CANCELLED) return o;
        if (o.getStatus() == OrderStatus.CONFIRMED) {
            // for now, block cancel after confirm (later we can support refunds/compensations)
            throw new IllegalArgumentException("Cannot cancel a confirmed order");
        }

        o.setStatus(OrderStatus.CANCELLED);
        OrderEntity saved = orderRepo.save(o);

        saveOutbox(saved.getId(), "OrderCancelled", Map.of(
                "orderId", saved.getId().toString(),
                "userId", userId,
                "reason", reason == null ? "" : reason
        ));
        return saved;
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