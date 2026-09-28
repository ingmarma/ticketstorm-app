package com.ticketstorm.shared.common.exception;

public class IdempotencyException extends DomainException {

    public IdempotencyException(String idempotencyKey) {
        super("IDEMPOTENCY_VIOLATION",
                "Duplicate operation detected for key: " + idempotencyKey);
    }
}
