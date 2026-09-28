package com.ticketstorm.shared.event.model;

import java.time.Instant;
import java.util.UUID;

public record EventCreated(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String aggregateType,
        String name,
        String description,
        String category,
        String venue,
        String city,
        Instant eventDate,
        Instant saleStart,
        Instant saleEnd
) implements DomainEvent {
    public EventCreated {
        aggregateType = "Event";
    }
}
