package com.ticketstorm.payment.infrastructure.messaging;

import com.ticketstorm.payment.application.service.PaymentService;
import com.ticketstorm.shared.event.model.ReservationCreated;
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

    private final PaymentService paymentService;

    @KafkaListener(topics = "reservation.created", groupId = "payment-service")
    public void handleReservationCreated(ConsumerRecord<String, String> record) {
        log.info("Received reservation.created: {}", record.value());
        try {
            ReservationCreated event = EventSerializer.deserialize(record.value(), ReservationCreated.class);
            paymentService.processPayment(
                    event.aggregateId(),
                    event.userId(),
                    event.totalAmount(),
                    event.currency(),
                    event.eventId().toString()
            );
        } catch (Exception e) {
            log.error("Failed to process reservation.created event", e);
        }
    }
}
