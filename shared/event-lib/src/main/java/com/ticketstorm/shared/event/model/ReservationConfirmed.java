package com.ticketstorm.shared.event.model;

import java.time.Instant;
import java.util.UUID;

public record ReservationConfirmed(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String aggregateType,
        String paymentId
) implements DomainEvent {
    public ReservationConfirmed {
        aggregateType = "Reservation";
    }
}
