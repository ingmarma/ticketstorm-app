package com.ticketstorm.shared.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SeatDTO(
        String id,
        String eventId,
        String sectionId,
        String sectionName,
        String seatNumber,
        SeatStatus status,
        BigDecimal price,
        String currency,
        Instant reservedAt,
        Instant expiresAt,
        String reservationId
) {
    public enum SeatStatus {
        AVAILABLE, BLOCKED, RESERVED, CONFIRMED, RELEASED
    }

    public record SeatBlockRequest(
            String eventId,
            String sectionId,
            int quantity
    ) {}

    public record SeatReserveRequest(
            String eventId,
            String seatId,
            String userId,
            String idempotencyKey
    ) {}
}
