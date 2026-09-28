package com.ticketstorm.shared.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventResponse(
        String id,
        @NotBlank String name,
        String description,
        String category,
        String venue,
        String city,
        Instant eventDate,
        Instant saleStart,
        Instant saleEnd,
        BigDecimal minPrice,
        String currency,
        int totalSeats,
        int availableSeats,
        String imageUrl,
        Instant createdAt,
        Instant updatedAt
) {
    public record SearchQuery(
            @NotBlank String query,
            @NotNull Integer topK,
            Double threshold
    ) {}

    public record SearchResult(
            EventResponse event,
            double score,
            String explanation
    ) {}
}
