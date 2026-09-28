package com.ticketstorm.reservation.infrastructure.messaging;

import com.ticketstorm.reservation.application.service.ReservationService;
import com.ticketstorm.shared.event.model.PaymentApproved;
import com.ticketstorm.shared.event.model.PaymentFailed;
import com.ticketstorm.shared.event.serializer.EventSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventConsumer {

    private final ReservationService reservationService;

    @KafkaListener(topics = "payment.approved", groupId = "reservation-service")
    public void handlePaymentApproved(ConsumerRecord<String, String> record) {
        log.info("Received payment.approved: {}", record.value());
        try {
            PaymentApproved event = EventSerializer.deserialize(record.value(), PaymentApproved.class);
            reservationService.confirmReservation(
                    UUID.fromString(event.aggregateId()),
                    event.aggregateId()
            );
        } catch (Exception e) {
            log.error("Failed to process payment.approved event", e);
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "reservation-service")
    public void handlePaymentFailed(ConsumerRecord<String, String> record) {
        log.info("Received payment.failed: {}", record.value());
        try {
            PaymentFailed event = EventSerializer.deserialize(record.value(), PaymentFailed.class);
            reservationService.failReservation(
                    UUID.fromString(event.aggregateId()),
                    event.reason()
            );
        } catch (Exception e) {
            log.error("Failed to process payment.failed event", e);
        }
    }
}
