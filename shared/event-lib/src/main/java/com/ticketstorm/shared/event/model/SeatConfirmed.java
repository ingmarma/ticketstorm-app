package com.ticketstorm.shared.event.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SeatConfirmed(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String aggregateType,
        String seatId,
        String sectionId,
        String reservationId,
        String paymentId,
        BigDecimal price,
        String currency
) implements DomainEvent {
    public SeatConfirmed {
        aggregateType = "Inventory";
    }
}
