package com.ticketstorm.shared.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueDTO(
        String eventId,
        String userId,
        int position,
        int totalInQueue,
        long estimatedWaitSeconds,
        QueueStatus status,
        Instant joinedAt,
        Instant turnGrantedAt,
        String turnToken
) {
    public enum QueueStatus {
        WAITING, IN_TURN, EXPIRED, CANCELLED
    }

    public record JoinQueueRequest(
            String eventId
    ) {}

    public record QueuePositionResponse(
            int position,
            int totalInQueue,
            long estimatedWaitSeconds,
            QueueStatus status
    ) {}
}
