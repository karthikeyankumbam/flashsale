package com.flashsale.order.api;

import com.flashsale.order.api.dto.CreateOrderRequest;
import com.flashsale.order.api.dto.CreateOrderResponse;
import com.flashsale.order.domain.OrderEntity;
import com.flashsale.order.service.OrderService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Orders")
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @Operation(summary = "Create an order", description = "Returns an initial status; check the order again after inventory and payment processing.")
    @PostMapping
    public CreateOrderResponse create(
            @Parameter(description = "Use the same user ID as your cart.", example = "U1001") @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "Use a fresh key for each new order. Reuse only for a retry of that order.", example = "manual-order-001") @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest req
    ) {
        OrderEntity order = service.createOrder(userId, idempotencyKey, req);
        return new CreateOrderResponse(order.getId().toString(), order.getStatus().name());
    }

    @Operation(summary = "Get an order and its current status")
    @GetMapping("/{orderId}")
    public OrderEntity get(
            @Parameter(description = "Use the same user ID as your cart.", example = "U1001") @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID orderId
    ) {
        return service.getOrder(userId, orderId);
    }

    @Operation(summary = "List orders belonging to a user")
    @GetMapping
    public List<OrderEntity> list(@Parameter(description = "Use the same user ID as your cart.", example = "U1001") @RequestHeader("X-User-Id") String userId) {
        return service.listOrders(userId);
    }

    @Operation(summary = "Request order cancellation")
    @PostMapping("/{orderId}/cancel")
    public OrderEntity cancel(
            @Parameter(description = "Use the same user ID as your cart.", example = "U1001") @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID orderId,
            @RequestParam(required = false) String reason
    ) {
        return service.cancelOrder(userId, orderId, reason);
    }
}
