package com.flashsale.order.api;

import com.flashsale.order.api.dto.CreateOrderRequest;
import com.flashsale.order.api.dto.CreateOrderResponse;
import com.flashsale.order.domain.OrderEntity;
import com.flashsale.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public CreateOrderResponse create(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest req
    ) {
        OrderEntity order = service.createOrder(userId, idempotencyKey, req);
        return new CreateOrderResponse(order.getId().toString(), order.getStatus().name());
    }

    @GetMapping("/{orderId}")
    public OrderEntity get(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID orderId
    ) {
        return service.getOrder(userId, orderId);
    }
}