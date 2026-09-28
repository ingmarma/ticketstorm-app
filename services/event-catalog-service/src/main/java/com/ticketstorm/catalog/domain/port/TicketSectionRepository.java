package com.ticketstorm.catalog.domain.port;

import com.ticketstorm.catalog.domain.model.TicketSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketSectionRepository extends JpaRepository<TicketSection, UUID> {
    List<TicketSection> findByEventIdOrderBySortOrderAsc(UUID eventId);
}
