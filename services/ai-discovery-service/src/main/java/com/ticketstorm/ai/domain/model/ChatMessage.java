package com.ticketstorm.ai.domain.model;

import java.time.Instant;

public record ChatMessage(
        String sessionId,
        String role,
        String content,
        Instant timestamp
) {}
