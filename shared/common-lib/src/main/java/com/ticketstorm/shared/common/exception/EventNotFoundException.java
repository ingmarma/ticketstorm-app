package com.ticketstorm.shared.common.exception;

public class EventNotFoundException extends DomainException {

    public EventNotFoundException(String eventId) {
        super("EVENT_NOT_FOUND", "Event not found: " + eventId);
    }
}
