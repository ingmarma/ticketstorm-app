package com.ticketstorm.notification.infrastructure.messaging;

import com.ticketstorm.notification.application.service.NotificationService;
import com.ticketstorm.shared.event.model.PaymentApproved;
import com.ticketstorm.shared.event.model.ReservationCancelled;
import com.ticketstorm.shared.event.model.ReservationConfirmed;
import com.ticketstorm.shared.event.serializer.EventSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "reservation.confirmed", groupId = "notification-service")
    public void handleReservationConfirmed(ConsumerRecord<String, String> record) {
        log.info("Received reservation.confirmed: {}", record.value());
        try {
            ReservationConfirmed event = EventSerializer.deserialize(record.value(), ReservationConfirmed.class);
            notificationService.handleReservationConfirmed(event);
        } catch (Exception e) {
            log.error("Failed to process reservation.confirmed event", e);
        }
    }

    @KafkaListener(topics = "reservation.cancelled", groupId = "notification-service")
    public void handleReservationCancelled(ConsumerRecord<String, String> record) {
        log.info("Received reservation.cancelled: {}", record.value());
        try {
            ReservationCancelled event = EventSerializer.deserialize(record.value(), ReservationCancelled.class);
            notificationService.handleReservationCancelled(event);
        } catch (Exception e) {
            log.error("Failed to process reservation.cancelled event", e);
        }
    }

    @KafkaListener(topics = "payment.approved", groupId = "notification-service")
    public void handlePaymentApproved(ConsumerRecord<String, String> record) {
        log.info("Received payment.approved: {}", record.value());
        try {
            PaymentApproved event = EventSerializer.deserialize(record.value(), PaymentApproved.class);
            notificationService.handlePaymentApproved(event);
        } catch (Exception e) {
            log.error("Failed to process payment.approved event", e);
        }
    }
}
