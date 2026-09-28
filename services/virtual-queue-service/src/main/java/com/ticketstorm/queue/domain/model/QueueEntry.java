package com.ticketstorm.queue.domain.model;

import java.time.Instant;

public record QueueEntry(
        String userId,
        String eventId,
        int position,
        Instant joinedAt,
        QueueStatus status
) {
    public enum QueueStatus {
        WAITING, IN_TURN, EXPIRED, CANCELLED
    }
}
