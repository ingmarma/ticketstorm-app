package com.ticketstorm.shared.event.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SeatReserved(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String aggregateType,
        String seatId,
        String sectionId,
        String reservationId,
        String userId,
        BigDecimal price,
        String currency
) implements DomainEvent {
    public SeatReserved {
        aggregateType = "Inventory";
    }
}
