package com.flashsale.order.events;

public record PaymentResultEvent(
        String orderId,
        String status,   // SUCCEEDED / FAILED
        String reason,
        String paymentId
) {}