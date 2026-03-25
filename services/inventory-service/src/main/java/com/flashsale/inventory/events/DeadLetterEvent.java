package com.flashsale.inventory.events;

import java.time.Instant;

public record DeadLetterEvent(
        String sourceTopic,
        String consumerGroup,
        String key,
        String error,
        String rawPayload,
        Instant occurredAt
) {}