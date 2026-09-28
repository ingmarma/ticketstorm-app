package com.ticketstorm.ai.application.dto;

import java.util.List;

public record ChatResponse(
        String response,
        List<EventDto> events,
        List<TicketSectionDto> sections,
        ChatAction action,
        String sessionId
) {}
