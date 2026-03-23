package com.flashsale.inventory.events;

public record InventoryResultEvent(
        String orderId,
        String status,        // RESERVED or REJECTED
        String reason,        // null if reserved
        String reservationId  // optional
) {}