package com.ticketstorm.shared.event.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FraudCheckCompleted(
        UUID eventId,
        Instant occurredAt,
        String aggregateId,
        String aggregateType,
        String userId,
        double score,
        String verdict,
        String reason,
        List<String> signals
) implements DomainEvent {
    public FraudCheckCompleted {
        aggregateType = "FraudDetection";
    }
}
