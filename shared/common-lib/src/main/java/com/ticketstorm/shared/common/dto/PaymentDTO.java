package com.ticketstorm.shared.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentDTO(
        String id,
        String reservationId,
        String userId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String idempotencyKey,
        Instant createdAt,
        Instant completedAt,
        String failureReason
) {
    public enum PaymentStatus {
        PENDING, PROCESSING, APPROVED, FAILED, REFUNDED
    }

    public record PaymentRequest(
            String reservationId,
            BigDecimal amount,
            String currency,
            String idempotencyKey
    ) {}
}
