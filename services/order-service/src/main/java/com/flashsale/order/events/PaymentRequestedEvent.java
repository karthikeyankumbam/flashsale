package com.flashsale.order.events;

public record PaymentRequestedEvent(
        String orderId,
        long amount,
        String currency
) {}