package com.ticketstorm.shared.event.model;

import java.time.Instant;
import java.util.UUID;

public record PaymentApproved(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String aggregateType,
        String reservationId,
        double fraudScore
) implements DomainEvent {
    public PaymentApproved {
        aggregateType = "Payment";
    }
}
