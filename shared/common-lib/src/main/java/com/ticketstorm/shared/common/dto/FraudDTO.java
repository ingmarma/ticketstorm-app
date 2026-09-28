package com.ticketstorm.shared.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FraudDTO(
        double score,
        FraudVerdict verdict,
        String reason,
        List<String> signals,
        String userId,
        String requestId
) {
    public enum FraudVerdict {
        APPROVED, REVIEW, REJECTED
    }

    public record FraudCheckRequest(
            String userId,
            String reservationId,
            String eventId,
            String ipAddress,
            String deviceFingerprint,
            java.math.BigDecimal amount
    ) {}
}
