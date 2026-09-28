package com.ticketstorm.shared.event.model;

import java.time.Instant;
import java.util.UUID;

public record ReservationCancelled(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String aggregateType,
        String reason,
        String cancelledBy
) implements DomainEvent {
    public ReservationCancelled {
        aggregateType = "Reservation";
    }
}
