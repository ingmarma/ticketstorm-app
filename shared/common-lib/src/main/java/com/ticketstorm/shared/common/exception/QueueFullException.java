package com.ticketstorm.shared.common.exception;

public class QueueFullException extends DomainException {

    public QueueFullException(String eventId) {
        super("QUEUE_FULL", "Queue is full for event: " + eventId);
    }
}
