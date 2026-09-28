package com.ticketstorm.shared.event.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SeatBlocked(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String aggregateType,
        String seatId,
        String sectionId,
        BigDecimal price,
        String currency
) implements DomainEvent {
    public SeatBlocked {
        aggregateType = "Inventory";
    }
}
