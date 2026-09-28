package com.ticketstorm.inventory.infrastructure.messaging;

import com.ticketstorm.shared.event.model.SeatConfirmed;
import com.ticketstorm.shared.event.model.SeatReleased;
import com.ticketstorm.shared.event.model.SeatReserved;
import com.ticketstorm.shared.event.serializer.EventSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class KafkaEventProducer {

    private static final String TOPIC_SEAT_RESERVED = "seat.reserved";
    private static final String TOPIC_SEAT_RELEASED = "seat.released";
    private static final String TOPIC_SEAT_CONFIRMED = "seat.confirmed";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishSeatReserved(String eventId, String seatId, String sectionId,
                                     String reservationId, String userId, Instant occurredAt) {
        SeatReserved event = new SeatReserved(
                UUID.randomUUID(), occurredAt, seatId, "Inventory",
                seatId, sectionId, reservationId, userId,
                java.math.BigDecimal.ZERO, "USD");

        String key = "inventory:" + seatId;
        kafkaTemplate.send(TOPIC_SEAT_RESERVED, key, EventSerializer.serialize(event))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish SeatReserved for seat={}: {}", seatId, ex.getMessage());
                    } else {
                        log.debug("Published SeatReserved for seat={}", seatId);
                    }
                });
    }

    public void publishSeatReleased(String eventId, String seatId, String sectionId,
                                     String reason, Instant occurredAt) {
        SeatReleased event = new SeatReleased(
                UUID.randomUUID(), occurredAt, seatId, "Inventory",
                seatId, sectionId, reason);

        String key = "inventory:" + seatId;
        kafkaTemplate.send(TOPIC_SEAT_RELEASED, key, EventSerializer.serialize(event))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish SeatReleased for seat={}: {}", seatId, ex.getMessage());
                    } else {
                        log.debug("Published SeatReleased for seat={}", seatId);
                    }
                });
    }

    public void publishSeatConfirmed(String eventId, String seatId, String sectionId,
                                      String reservationId, String paymentId, Instant occurredAt) {
        SeatConfirmed event = new SeatConfirmed(
                UUID.randomUUID(), occurredAt, seatId, "Inventory",
                seatId, sectionId, reservationId, paymentId,
                java.math.BigDecimal.ZERO, "USD");

        String key = "inventory:" + seatId;
        kafkaTemplate.send(TOPIC_SEAT_CONFIRMED, key, EventSerializer.serialize(event))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish SeatConfirmed for seat={}: {}", seatId, ex.getMessage());
                    } else {
                        log.debug("Published SeatConfirmed for seat={}", seatId);
                    }
                });
    }
}
