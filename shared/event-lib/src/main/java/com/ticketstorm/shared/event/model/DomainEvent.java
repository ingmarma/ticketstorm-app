package com.ticketstorm.shared.event.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = EventCreated.class, name = "EventCreated"),
        @JsonSubTypes.Type(value = EventUpdated.class, name = "EventUpdated"),
        @JsonSubTypes.Type(value = SeatBlocked.class, name = "SeatBlocked"),
        @JsonSubTypes.Type(value = SeatReserved.class, name = "SeatReserved"),
        @JsonSubTypes.Type(value = SeatReleased.class, name = "SeatReleased"),
        @JsonSubTypes.Type(value = SeatConfirmed.class, name = "SeatConfirmed"),
        @JsonSubTypes.Type(value = ReservationCreated.class, name = "ReservationCreated"),
        @JsonSubTypes.Type(value = ReservationConfirmed.class, name = "ReservationConfirmed"),
        @JsonSubTypes.Type(value = ReservationCancelled.class, name = "ReservationCancelled"),
        @JsonSubTypes.Type(value = ReservationExpired.class, name = "ReservationExpired"),
        @JsonSubTypes.Type(value = PaymentRequested.class, name = "PaymentRequested"),
        @JsonSubTypes.Type(value = PaymentApproved.class, name = "PaymentApproved"),
        @JsonSubTypes.Type(value = PaymentFailed.class, name = "PaymentFailed"),
        @JsonSubTypes.Type(value = NotificationSent.class, name = "NotificationSent"),
        @JsonSubTypes.Type(value = FraudCheckCompleted.class, name = "FraudCheckCompleted")
})
public sealed interface DomainEvent
        permits EventCreated, EventUpdated,
                SeatBlocked, SeatReserved, SeatReleased, SeatConfirmed,
                ReservationCreated, ReservationConfirmed, ReservationCancelled, ReservationExpired,
                PaymentRequested, PaymentApproved, PaymentFailed,
                NotificationSent, FraudCheckCompleted {

    UUID eventId();
    Instant occurredAt();
    String aggregateId();
    String aggregateType();
}
