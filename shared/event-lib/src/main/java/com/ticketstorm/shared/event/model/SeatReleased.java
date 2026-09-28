package com.ticketstorm.shared.event.model;

import java.time.Instant;
import java.util.UUID;

public record SeatReleased(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String aggregateType,
        String seatId,
        String sectionId,
        String reason
) implements DomainEvent {
    public SeatReleased {
        aggregateType = "Inventory";
    }
}
