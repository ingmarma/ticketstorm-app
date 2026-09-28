package com.ticketstorm.ai.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KafkaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumer.class);

    private final Map<String, String> recentEvents = new ConcurrentHashMap<>();

    @KafkaListener(topics = "event.created", groupId = "ai-discovery-service")
    public void onEventCreated(String payload) {
        log.info("Received event.created: {}", payload);
        recentEvents.put("latest_event", payload);
    }

    @KafkaListener(topics = "reservation.confirmed", groupId = "ai-discovery-service")
    public void onReservationConfirmed(String payload) {
        log.info("Received reservation.confirmed: {}", payload);
    }

    @KafkaListener(topics = "payment.approved", groupId = "ai-discovery-service")
    public void onPaymentApproved(String payload) {
        log.info("Received payment.approved: {}", payload);
    }

    public Map<String, String> getRecentEvents() {
        return Map.copyOf(recentEvents);
    }
}
