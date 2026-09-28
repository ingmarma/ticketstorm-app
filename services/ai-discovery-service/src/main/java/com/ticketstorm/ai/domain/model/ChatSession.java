package com.ticketstorm.ai.domain.model;

import com.ticketstorm.shared.common.dto.FraudDTO;

import java.time.Instant;

public record ChatSession(
        String sessionId,
        String userId,
        Instant createdAt,
        Instant lastActiveAt
) {}
