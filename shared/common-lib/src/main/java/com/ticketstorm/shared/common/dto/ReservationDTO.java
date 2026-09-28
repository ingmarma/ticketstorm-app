package com.ticketstorm.shared.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservationDTO(
        String id,
        String eventId,
        String userId,
        String sectionId,
        int quantity,
        BigDecimal totalAmount,
        String currency,
        ReservationStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant confirmedAt
) {
    public enum ReservationStatus {
        PENDING, CONFIRMED, CANCELLED, EXPIRED, PAYMENT_FAILED
    }

    public record CreateReservationRequest(
            String eventId,
            String sectionId,
            int quantity,
            String idempotencyKey
    ) {}
}
