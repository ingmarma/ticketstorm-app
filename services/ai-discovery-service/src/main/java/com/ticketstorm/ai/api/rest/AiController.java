package com.ticketstorm.ai.api.rest;

import com.ticketstorm.ai.application.dto.ChatRequest;
import com.ticketstorm.ai.application.dto.ChatResponse;
import com.ticketstorm.ai.application.service.ChatService;
import com.ticketstorm.ai.application.service.FraudDetectionService;
import com.ticketstorm.ai.application.service.SemanticSearchService;
import com.ticketstorm.shared.common.dto.EventResponse;
import com.ticketstorm.shared.common.dto.FraudDTO;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final SemanticSearchService searchService;
    private final ChatService chatService;
    private final FraudDetectionService fraudService;
    private final MeterRegistry meterRegistry;

    public AiController(SemanticSearchService searchService,
                        ChatService chatService,
                        FraudDetectionService fraudService,
                        MeterRegistry meterRegistry) {
        this.searchService = searchService;
        this.chatService = chatService;
        this.fraudService = fraudService;
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/search")
    public ResponseEntity<List<EventResponse.SearchResult>> searchGet(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "0.5") double threshold) {
        log.info("GET /api/v1/ai/search?q={}, topK={}, threshold={}", q, topK, threshold);
        List<EventResponse.SearchResult> results = searchService.search(q, topK, threshold);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/search")
    public ResponseEntity<List<EventResponse.SearchResult>> searchPost(
            @RequestBody EventResponse.SearchQuery query) {
        log.info("POST /api/v1/ai/search: {}", query);
        List<EventResponse.SearchResult> results = searchService.search(
                query.query(),
                query.topK() != null ? query.topK() : 5,
                query.threshold() != null ? query.threshold() : 0.5
        );
        return ResponseEntity.ok(results);
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", UUID.randomUUID().toString());
        String message = request.get("message");

        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is required"));
        }

        ChatResponse response = chatService.chat(new ChatRequest(message, sessionId));
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @RequestParam String sessionId,
            @RequestParam String message) {
        log.info("SSE /api/v1/ai/chat/stream: sessionId={}, message='{}'", sessionId, message);
        return chatService.chatStream(sessionId, message);
    }

    @PostMapping("/fraud-check")
    public ResponseEntity<FraudDTO> fraudCheck(@RequestBody FraudDTO.FraudCheckRequest request) {
        log.info("POST /api/v1/ai/fraud-check: userId={}, amount={}", request.userId(), request.amount());
        FraudDTO result = fraudService.checkFraud(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "ai-discovery-service",
                "version", "1.0.0"
        ));
    }
}
