package com.ticketstorm.ai.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record EventDto(
        String id,
        String name,
        String description,
        String venue,
        String city,
        String category,
        Instant eventDate,
        BigDecimal minPrice,
        String currency,
        String imageUrl,
        int totalSeats,
        int availableSeats,
        Instant createdAt,
        Instant updatedAt
) {}
