package com.ticketstorm.shared.common.exception;

public class ReservationExpiredException extends DomainException {

    public ReservationExpiredException(String reservationId) {
        super("RESERVATION_EXPIRED", "Reservation has expired: " + reservationId);
    }
}
