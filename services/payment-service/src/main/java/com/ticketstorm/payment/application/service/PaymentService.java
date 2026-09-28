package com.ticketstorm.payment.application.service;

import com.ticketstorm.payment.domain.model.Payment;
import com.ticketstorm.payment.domain.model.Payment.PaymentStatus;
import com.ticketstorm.payment.domain.port.PaymentRepository;
import com.ticketstorm.payment.infrastructure.messaging.KafkaEventProducer;
import com.ticketstorm.shared.event.model.PaymentApproved;
import com.ticketstorm.shared.event.model.PaymentFailed;
import com.ticketstorm.shared.event.serializer.EventSerializer;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaEventProducer kafkaEventProducer;
    private final Random random = new Random();

    @Transactional(readOnly = true)
    public Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    }

    @Timed(value = "payment.process", description = "Payment processing time")
    @Transactional
    public Payment processPayment(String reservationId, String userId,
                                   java.math.BigDecimal amount, String currency,
                                   String idempotencyKey) {
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent payment request for key {}", idempotencyKey);
            return existing.get();
        }

        Payment payment = Payment.builder()
                .reservationId(reservationId)
                .userId(userId)
                .amount(amount)
                .currency(currency)
                .status(PaymentStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        CompletableFuture.runAsync(() -> processPaymentAsync(savedPayment));

        log.info("Payment {} initiated for reservation {}", savedPayment.getId(), reservationId);
        return savedPayment;
    }

    private void processPaymentAsync(Payment payment) {
        try {
            payment.setStatus(PaymentStatus.PROCESSING);
            paymentRepository.save(payment);

            TimeUnit.MILLISECONDS.sleep(500);

            boolean isSuccess = random.nextDouble() > 0.3;

            if (isSuccess) {
                payment.setStatus(PaymentStatus.APPROVED);
                payment.setCompletedAt(Instant.now());
                paymentRepository.save(payment);

                PaymentApproved event = new PaymentApproved(
                        UUID.randomUUID(),
                        Instant.now(),
                        payment.getId().toString(),
                        "Payment",
                        payment.getReservationId(),
                        0.0
                );
                kafkaEventProducer.publish("payment.approved", event);

                log.info("Payment {} approved", payment.getId());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Simulated payment failure");
                payment.setCompletedAt(Instant.now());
                paymentRepository.save(payment);

                PaymentFailed event = new PaymentFailed(
                        UUID.randomUUID(),
                        Instant.now(),
                        payment.getId().toString(),
                        "Payment",
                        payment.getReservationId(),
                        "Simulated payment failure",
                        0.0
                );
                kafkaEventProducer.publish("payment.failed", event);

                log.info("Payment {} failed", payment.getId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Payment processing interrupted for {}", payment.getId(), e);
        }
    }
}
