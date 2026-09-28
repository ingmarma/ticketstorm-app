package com.ticketstorm.ai.domain.port;

import com.ticketstorm.ai.domain.model.InventoryCheckResult;

import java.util.Optional;

public interface InventoryPort {
    Optional<InventoryCheckResult> checkAvailability(String eventId, String sectionId);
    boolean reserveSeat(String eventId, String seatId, String userId);
}
