package com.ticketstorm.ai.domain.model;

import java.math.BigDecimal;

public record InventoryCheckResult(
        String eventId,
        String sectionId,
        int availableSeats,
        BigDecimal price,
        String currency
) {}
