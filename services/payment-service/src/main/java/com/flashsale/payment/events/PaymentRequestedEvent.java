package com.flashsale.payment.events;

public record PaymentRequestedEvent(
        String orderId,
        long amount,
        String currency
) {}