package com.ticketstorm.shared.event.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRequested(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String aggregateType,
        String reservationId,
        BigDecimal amount,
        String currency,
        String idempotencyKey
) implements DomainEvent {
    public PaymentRequested {
        aggregateType = "Payment";
    }
}
