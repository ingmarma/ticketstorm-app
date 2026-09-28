package com.ticketstorm.ai.application.dto;

import java.math.BigDecimal;

public record TicketSectionDto(
        String id,
        String eventId,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int totalCapacity,
        int availableCapacity,
        int sortOrder
) {}
