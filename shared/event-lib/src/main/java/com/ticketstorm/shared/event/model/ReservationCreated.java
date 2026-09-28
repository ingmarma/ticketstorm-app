package com.ticketstorm.shared.event.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReservationCreated(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String aggregateType,
        String userId,
        String sectionId,
        int quantity,
        BigDecimal totalAmount,
        String currency,
        Instant expiresAt
) implements DomainEvent {
    public ReservationCreated {
        aggregateType = "Reservation";
    }
}
