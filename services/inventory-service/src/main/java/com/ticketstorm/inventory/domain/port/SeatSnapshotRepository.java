package com.ticketstorm.inventory.domain.port;

import com.ticketstorm.inventory.domain.model.SeatSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SeatSnapshotRepository extends JpaRepository<SeatSnapshot, Long> {

    Optional<SeatSnapshot> findByEventIdAndSeatId(String eventId, String seatId);

    Optional<SeatSnapshot> findBySeatId(String seatId);

    Optional<SeatSnapshot> findByCurrentReservationId(String reservationId);

    List<SeatSnapshot> findByEventId(String eventId);

    List<SeatSnapshot> findByEventIdAndSectionId(String eventId, String sectionId);

    @Query("SELECT COUNT(s) FROM SeatSnapshot s WHERE s.eventId = :eventId AND s.sectionId = :sectionId AND s.status = 'AVAILABLE'")
    long countAvailableByEventIdAndSectionId(String eventId, String sectionId);
}
