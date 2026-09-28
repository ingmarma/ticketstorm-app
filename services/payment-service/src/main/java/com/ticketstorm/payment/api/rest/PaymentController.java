package com.ticketstorm.payment.api.rest;

import com.ticketstorm.payment.application.service.PaymentService;
import com.ticketstorm.payment.domain.model.Payment;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Timed(value = "payment.controller", description = "Payment controller metrics")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Payment> processPayment(@RequestBody PaymentRequest request) {
        Payment payment = paymentService.processPayment(
                request.reservationId(),
                request.userId(),
                request.amount(),
                request.currency(),
                request.idempotencyKey()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable UUID id) {
        Payment payment = paymentService.getPayment(id);
        return ResponseEntity.ok(payment);
    }

    public record PaymentRequest(
            String reservationId,
            String userId,
            BigDecimal amount,
            String currency,
            String idempotencyKey
    ) {}
}
