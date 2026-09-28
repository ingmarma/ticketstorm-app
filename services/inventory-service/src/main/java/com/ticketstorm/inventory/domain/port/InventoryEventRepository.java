package com.ticketstorm.inventory.domain.port;

import com.ticketstorm.inventory.domain.model.InventoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryEventRepository extends JpaRepository<InventoryEvent, Long> {

    List<InventoryEvent> findByEventIdAndSeatId(String eventId, String seatId);

    long countByEventIdAndSeatIdAndEventType(String eventId, String seatId, String eventType);
}
