package com.ticketstorm.reservation.domain.port;

import com.ticketstorm.reservation.domain.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);

    List<Reservation> findByUserId(String userId);

    List<Reservation> findByEventId(String eventId);

    @Query("SELECT r FROM Reservation r WHERE r.status = 'PENDING' AND r.expiresAt < CURRENT_TIMESTAMP")
    List<Reservation> findExpiredReservations();
}
