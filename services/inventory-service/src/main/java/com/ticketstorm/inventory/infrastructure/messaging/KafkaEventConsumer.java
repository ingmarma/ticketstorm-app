package com.ticketstorm.inventory.infrastructure.messaging;

import com.ticketstorm.inventory.application.service.InventoryService;
import com.ticketstorm.shared.event.serializer.EventSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class KafkaEventConsumer {

    private final InventoryService inventoryService;

    public KafkaEventConsumer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = "reservation.created", groupId = "inventory-service")
    public void handleReservationCreated(String message, Acknowledgment ack) {
        try {
            Map<String, String> payload = EventSerializer.getMapper().readValue(message, Map.class);
            String eventId = payload.get("eventId");
            String sectionId = payload.get("sectionId");
            String quantityStr = payload.get("quantity");

            log.info("Received reservation.created for event={} section={}", eventId, sectionId);

            if (quantityStr != null && eventId != null && sectionId != null) {
                int quantity = Integer.parseInt(quantityStr);
                inventoryService.blockSeats(eventId, sectionId, quantity);
            }
        } catch (Exception e) {
            log.error("Failed to process reservation.created: {}", e.getMessage(), e);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "payment.approved", groupId = "inventory-service")
    public void handlePaymentApproved(String message, Acknowledgment ack) {
        try {
            Map<String, String> payload = EventSerializer.getMapper().readValue(message, Map.class);
            String reservationId = payload.get("reservationId");
            String paymentId = payload.getOrDefault("paymentId", "unknown");

            log.info("Received payment.approved for reservation={}", reservationId);

            if (reservationId != null) {
                inventoryService.confirmSeat(reservationId, paymentId);
            }
        } catch (Exception e) {
            log.error("Failed to process payment.approved: {}", e.getMessage(), e);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "inventory-service")
    public void handlePaymentFailed(String message, Acknowledgment ack) {
        try {
            Map<String, String> payload = EventSerializer.getMapper().readValue(message, Map.class);
            String reservationId = payload.get("reservationId");
            String reason = payload.getOrDefault("reason", "payment_failed");

            log.info("Received payment.failed for reservation={}", reservationId);

            if (reservationId != null) {
                inventoryService.releaseSeat(reservationId, reason);
            }
        } catch (Exception e) {
            log.error("Failed to process payment.failed: {}", e.getMessage(), e);
        } finally {
            ack.acknowledge();
        }
    }
}
