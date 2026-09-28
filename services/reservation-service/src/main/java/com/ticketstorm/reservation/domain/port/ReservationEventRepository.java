package com.ticketstorm.reservation.domain.port;

import com.ticketstorm.reservation.domain.model.ReservationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationEventRepository extends JpaRepository<ReservationEvent, Long> {

    List<ReservationEvent> findByReservationIdOrderByTimestampAsc(UUID reservationId);
}
