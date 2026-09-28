package com.ticketstorm.shared.event.model;

import java.time.Instant;
import java.util.UUID;

public record ReservationExpired(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String aggregateType
) implements DomainEvent {
    public ReservationExpired {
        aggregateType = "Reservation";
    }
}
