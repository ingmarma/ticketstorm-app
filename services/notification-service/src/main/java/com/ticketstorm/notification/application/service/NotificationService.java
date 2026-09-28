package com.ticketstorm.notification.application.service;

import com.ticketstorm.shared.event.model.*;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final ConcurrentLinkedDeque<NotificationRecord> recentNotifications = new ConcurrentLinkedDeque<>();
    private static final int MAX_RECENT = 100;

    @Timed(value = "notification.send", description = "Notification sending time")
    public void handleReservationConfirmed(ReservationConfirmed event) {
        String message = "Your reservation %s has been confirmed. Payment ID: %s".formatted(
                event.aggregateId(), event.paymentId());
        sendNotification(event.aggregateId(), "RESERVATION_CONFIRMED", message);
    }

    @Timed(value = "notification.send", description = "Notification sending time")
    public void handleReservationCancelled(ReservationCancelled event) {
        String message = "Your reservation %s has been cancelled. Reason: %s".formatted(
                event.aggregateId(), event.reason());
        sendNotification(event.aggregateId(), "RESERVATION_CANCELLED", message);
    }

    @Timed(value = "notification.send", description = "Notification sending time")
    public void handlePaymentApproved(PaymentApproved event) {
        String message = "Payment for reservation %s has been approved.".formatted(event.aggregateId());
        sendNotification(event.reservationId(), "PAYMENT_APPROVED", message);
    }

    private void sendNotification(String userId, String type, String message) {
        log.info("Sending notification to user {}: [{}] {}", userId, type, message);

        NotificationRecord record = new NotificationRecord(
                userId,
                type,
                message,
                Instant.now()
        );

        recentNotifications.addFirst(record);
        while (recentNotifications.size() > MAX_RECENT) {
            recentNotifications.removeLast();
        }

        log.info("Notification sent successfully: {}", type);
    }

    public List<NotificationRecord> getRecentNotifications() {
        return List.copyOf(recentNotifications);
    }

    public record NotificationRecord(
            String userId,
            String type,
            String message,
            Instant sentAt
    ) {}
}
