package com.ticketstorm.reservation.api.rest;

import com.ticketstorm.reservation.application.service.ReservationService;
import com.ticketstorm.reservation.domain.model.Reservation;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Timed(value = "reservation.controller", description = "Reservation controller metrics")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<Reservation> createReservation(
            @RequestBody CreateReservationRequest request) {
        Reservation reservation = reservationService.createReservation(
                request.eventId(),
                request.userId(),
                request.sectionId(),
                request.quantity(),
                request.totalAmount(),
                request.currency(),
                request.idempotencyKey()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getReservation(@PathVariable UUID id) {
        Reservation reservation = reservationService.getReservation(id);
        return ResponseEntity.ok(reservation);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Reservation> cancelReservation(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "User cancelled") : "User cancelled";
        Reservation reservation = reservationService.cancelReservation(id, reason, "user");
        return ResponseEntity.ok(reservation);
    }

    public record CreateReservationRequest(
            String eventId,
            String userId,
            String sectionId,
            int quantity,
            BigDecimal totalAmount,
            String currency,
            String idempotencyKey
    ) {}
}
