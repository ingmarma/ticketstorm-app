package com.ticketstorm.reservation.application.service;

import com.ticketstorm.reservation.domain.model.Reservation;
import com.ticketstorm.reservation.domain.model.Reservation.ReservationStatus;
import com.ticketstorm.reservation.domain.model.ReservationEvent;
import com.ticketstorm.reservation.domain.port.ReservationEventRepository;
import com.ticketstorm.reservation.domain.port.ReservationRepository;
import com.ticketstorm.reservation.infrastructure.messaging.KafkaEventProducer;
import com.ticketstorm.shared.event.model.*;
import com.ticketstorm.shared.event.serializer.EventSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationEventRepository reservationEventRepository;
    private final KafkaEventProducer kafkaEventProducer;

    @Transactional
    public Reservation createReservation(String eventId, String userId, String sectionId,
                                          int quantity, java.math.BigDecimal totalAmount,
                                          String currency, String idempotencyKey) {
        var existing = reservationRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent request for key {}", idempotencyKey);
            return existing.get();
        }

        Reservation reservation = Reservation.builder()
                .eventId(eventId)
                .userId(userId)
                .sectionId(sectionId)
                .quantity(quantity)
                .totalAmount(totalAmount)
                .currency(currency)
                .status(ReservationStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        reservation = reservationRepository.save(reservation);

        ReservationCreated event = new ReservationCreated(
                UUID.randomUUID(),
                Instant.now(),
                reservation.getId().toString(),
                "Reservation",
                userId,
                sectionId,
                quantity,
                totalAmount,
                currency,
                reservation.getExpiresAt()
        );

        saveEvent(reservation.getId(), "ReservationCreated", EventSerializer.serialize(event));
        kafkaEventProducer.publish("reservation.created", event);

        log.info("Created reservation {} for user {} event {}", reservation.getId(), userId, eventId);
        return reservation;
    }

    @Transactional(readOnly = true)
    public Reservation getReservation(UUID reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
    }

    @Transactional
    public Reservation confirmReservation(UUID reservationId, String paymentId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Cannot confirm reservation in status: " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setConfirmedAt(Instant.now());
        reservation = reservationRepository.save(reservation);

        ReservationConfirmed event = new ReservationConfirmed(
                UUID.randomUUID(),
                Instant.now(),
                reservation.getId().toString(),
                "Reservation",
                paymentId
        );

        saveEvent(reservation.getId(), "ReservationConfirmed", EventSerializer.serialize(event));
        kafkaEventProducer.publish("reservation.confirmed", event);

        log.info("Confirmed reservation {} with payment {}", reservationId, paymentId);
        return reservation;
    }

    @Transactional
    public Reservation cancelReservation(UUID reservationId, String reason, String cancelledBy) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel confirmed reservation");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation = reservationRepository.save(reservation);

        ReservationCancelled event = new ReservationCancelled(
                UUID.randomUUID(),
                Instant.now(),
                reservation.getId().toString(),
                "Reservation",
                reason,
                cancelledBy
        );

        saveEvent(reservation.getId(), "ReservationCancelled", EventSerializer.serialize(event));
        kafkaEventProducer.publish("reservation.cancelled", event);

        log.info("Cancelled reservation {} reason: {}", reservationId, reason);
        return reservation;
    }

    @Transactional
    public Reservation failReservation(UUID reservationId, String reason) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        reservation.setStatus(ReservationStatus.PAYMENT_FAILED);
        reservation = reservationRepository.save(reservation);

        log.info("Reservation {} marked as payment failed: {}", reservationId, reason);
        return reservation;
    }

    @Scheduled(fixedRate = 30_000)
    @Transactional
    public void expireReservations() {
        List<Reservation> expired = reservationRepository.findExpiredReservations();
        for (Reservation reservation : expired) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);

            ReservationExpired event = new ReservationExpired(
                    UUID.randomUUID(),
                    Instant.now(),
                    reservation.getId().toString(),
                    "Reservation"
            );

            saveEvent(reservation.getId(), "ReservationExpired", EventSerializer.serialize(event));
            kafkaEventProducer.publish("reservation.expired", event);

            log.info("Expired reservation {} for user {}", reservation.getId(), reservation.getUserId());
        }
    }

    private void saveEvent(UUID reservationId, String eventType, String payload) {
        ReservationEvent event = ReservationEvent.builder()
                .reservationId(reservationId)
                .eventType(eventType)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
        reservationEventRepository.save(event);
    }
}
