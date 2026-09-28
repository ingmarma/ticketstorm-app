package com.ticketstorm.ai.api.rest;

import com.ticketstorm.ai.application.dto.ChatRequest;
import com.ticketstorm.ai.application.dto.ChatResponse;
import com.ticketstorm.ai.application.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private static final List<String> SUGGESTIONS = List.of(
            "¿Qué conciertos hay en noviembre?",
            "¿Cuál es el evento más barato?",
            "Quiero ir a ver fútbol");

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message is required"));
        }
        log.info("POST /api/v1/chat: sessionId={}", request.sessionId());
        ChatResponse response = chatService.chat(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> suggestions() {
        return ResponseEntity.ok(SUGGESTIONS);
    }
}
