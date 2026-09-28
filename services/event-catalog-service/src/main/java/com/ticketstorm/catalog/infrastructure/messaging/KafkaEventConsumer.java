package com.ticketstorm.catalog.infrastructure.messaging;

import com.ticketstorm.shared.event.model.EventCreated;
import com.ticketstorm.shared.event.model.EventUpdated;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaEventConsumer {

    private final MeterRegistry meterRegistry;

    public KafkaEventConsumer(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(topics = "event.created", groupId = "event-catalog-service")
    public void onEventCreated(EventCreated event) {
        log.info("Received EventCreated for event {}", event.eventId());
        meterRegistry.counter("catalog.kafka.event.created").increment();
    }

    @KafkaListener(topics = "event.updated", groupId = "event-catalog-service")
    public void onEventUpdated(EventUpdated event) {
        log.info("Received EventUpdated for event {}", event.eventId());
        meterRegistry.counter("catalog.kafka.event.updated").increment();
    }
}
