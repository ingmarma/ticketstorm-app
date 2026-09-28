package com.ticketstorm.reservation.infrastructure.messaging;

import com.ticketstorm.shared.event.model.DomainEvent;
import com.ticketstorm.shared.event.serializer.EventSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(String topic, DomainEvent event) {
        String payload = EventSerializer.serialize(event);
        kafkaTemplate.send(topic, event.aggregateId(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event {} to topic {}", event.getClass().getSimpleName(), topic, ex);
                    } else {
                        log.debug("Published event {} to topic {}", event.getClass().getSimpleName(), topic);
                    }
                });
    }
}
