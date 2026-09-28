package com.ticketstorm.shared.common.exception;

public class SeatNotAvailableException extends DomainException {

    public SeatNotAvailableException(String seatId) {
        super("SEAT_NOT_AVAILABLE", "Seat is not available: " + seatId);
    }

    public SeatNotAvailableException(String seatId, String eventId) {
        super("SEAT_NOT_AVAILABLE",
                "Seat " + seatId + " is not available for event " + eventId);
    }
}
