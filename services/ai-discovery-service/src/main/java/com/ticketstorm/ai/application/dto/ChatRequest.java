package com.ticketstorm.ai.application.dto;

public record ChatRequest(
        String message,
        String sessionId
) {}
