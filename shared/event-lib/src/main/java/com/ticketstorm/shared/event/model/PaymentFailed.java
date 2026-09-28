package com.ticketstorm.shared.event.model;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailed(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String aggregateType,
        String reservationId,
        String reason,
        double fraudScore
) implements DomainEvent {
    public PaymentFailed {
        aggregateType = "Payment";
    }
}
