package com.flashsale.payment.events;

public record PaymentResultEvent(
        String orderId,
        String status,   // SUCCEEDED / FAILED
        String reason,   // null if success
        String paymentId // optional
) {}