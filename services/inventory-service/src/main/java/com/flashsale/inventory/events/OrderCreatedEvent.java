package com.flashsale.inventory.events;

import java.util.List;

public record OrderCreatedEvent(
        String orderId,
        String userId,
        List<Item> items,
        long total,
        String currency
) {
    public record Item(String sku, int qty, long unitPrice) {}
}