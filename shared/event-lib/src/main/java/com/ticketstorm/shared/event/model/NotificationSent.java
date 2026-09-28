package com.ticketstorm.shared.event.model;

import java.time.Instant;
import java.util.UUID;

public record NotificationSent(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String aggregateType,
        String userId,
        String channel,
        String template,
        boolean success
) implements DomainEvent {
    public NotificationSent {
        aggregateType = "Notification";
    }
}
