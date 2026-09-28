package com.ticketstorm.shared.event.model;

import java.time.Instant;
import java.util.UUID;

public record EventUpdated(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String aggregateType,
        String name,
        String description,
        String category,
        Instant eventDate
) implements DomainEvent {
    public EventUpdated {
        aggregateType = "Event";
    }
}
