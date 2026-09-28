package com.ticketstorm.ai.application.dto;

import java.util.List;

public record ChatResponse(
        String response,
        List<Event> events,
        String sessionId
) {}
