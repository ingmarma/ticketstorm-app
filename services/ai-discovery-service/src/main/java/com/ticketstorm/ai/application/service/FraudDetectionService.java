package com.ticketstorm.ai.application.service;

import com.ticketstorm.shared.common.dto.FraudDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);

    private final ChatClient chatClient;
    private final DistributionSummary fraudScoreSummary;
    private final Counter fraudCheckCounter;

    public FraudDetectionService(ChatClient.Builder chatClientBuilder, MeterRegistry meterRegistry) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are a fraud detection AI for a ticket reservation system.
                        Analyze the given request and return a JSON fraud assessment.
                        Return ONLY valid JSON with fields: score (0.0-1.0), verdict (APPROVED/REVIEW/REJECTED),
                        reason (string), signals (list of strings).
                        Score thresholds: >0.8 = REJECTED, 0.5-0.8 = REVIEW, <0.5 = APPROVED.
                        Consider: user history, IP reputation, device fingerprint, amount, booking patterns.
                        """)
                .build();
        this.fraudScoreSummary = DistributionSummary.builder("ai.fraud.score")
                .description("Fraud score distribution")
                .register(meterRegistry);
        this.fraudCheckCounter = Counter.builder("ai.fraud.checks.total")
                .description("Total fraud checks")
                .register(meterRegistry);
    }

    @CircuitBreaker(name = "bedrock", fallbackMethod = "fallbackFraudCheck")
    @Retry(name = "bedrock")
    @TimeLimiter(name = "bedrock")
    public FraudDTO checkFraud(FraudDTO.FraudCheckRequest request) {
        fraudCheckCounter.increment();
        log.info("Performing fraud check for user={}, reservation={}, amount={}",
                request.userId(), request.reservationId(), request.amount());

        String prompt = buildFraudPrompt(request);
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        FraudDTO result = parseFraudResponse(response, request.userId(), request.reservationId());
        fraudScoreSummary.record(result.score());
        log.info("Fraud check result: score={}, verdict={}", result.score(), result.verdict());
        return result;
    }

    private FraudDTO fallbackFraudCheck(FraudDTO.FraudCheckRequest request, Throwable t) {
        log.warn("Fraud check fallback triggered for user={}: {}", request.userId(), t.getMessage());
        return new FraudDTO(
                0.5,
                FraudDTO.FraudVerdict.REVIEW,
                "AI unavailable - manual review required",
                List.of("fallback:ai_unavailable"),
                request.userId(),
                request.reservationId()
        );
    }

    private String buildFraudPrompt(FraudDTO.FraudCheckRequest request) {
        return String.format("""
                Analyze this ticket purchase for fraud risk:

                User ID: %s
                Reservation ID: %s
                Event ID: %s
                IP Address: %s
                Device Fingerprint: %s
                Amount: %s %s

                Return ONLY a JSON object with: score, verdict, reason, signals.
                """,
                request.userId(), request.reservationId(), request.eventId(),
                request.ipAddress(), request.deviceFingerprint(),
                request.amount(), "PYG"
        );
    }

    private FraudDTO parseFraudResponse(String response, String userId, String requestId) {
        try {
            String cleaned = response.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```json\\n?", "").replaceAll("```\\n?", "").strip();
            }

            double score = extractDouble(cleaned, "\"score\"");
            String verdict = extractString(cleaned, "\"verdict\"");
            String reason = extractString(cleaned, "\"reason\"");

            FraudDTO.FraudVerdict v = switch (verdict != null ? verdict.toUpperCase() : "REVIEW") {
                case "APPROVED" -> FraudDTO.FraudVerdict.APPROVED;
                case "REJECTED" -> FraudDTO.FraudVerdict.REJECTED;
                default -> FraudDTO.FraudVerdict.REVIEW;
            };

            return new FraudDTO(score, v, reason, List.of("ai:bedrock_analysis"), userId, requestId);
        } catch (Exception e) {
            log.error("Failed to parse fraud response: {}", e.getMessage());
            return new FraudDTO(0.5, FraudDTO.FraudVerdict.REVIEW,
                    "Parse error - manual review required", List.of("error:parse_failed"),
                    userId, requestId);
        }
    }

    private double extractDouble(String json, String field) {
        int idx = json.indexOf(field);
        if (idx < 0) return 0.5;
        String after = json.substring(idx + field.length()).replaceFirst(":", "").strip();
        StringBuilder num = new StringBuilder();
        for (char c : after.toCharArray()) {
            if (Character.isDigit(c) || c == '.') num.append(c);
            else break;
        }
        return num.isEmpty() ? 0.5 : Double.parseDouble(num.toString());
    }

    private String extractString(String json, String field) {
        int idx = json.indexOf(field);
        if (idx < 0) return "";
        String after = json.substring(idx + field.length()).replaceFirst(":", "").strip();
        if (after.startsWith("\"")) {
            int end = after.indexOf("\"", 1);
            return end > 0 ? after.substring(1, end) : after.substring(1);
        }
        return "";
    }
}
