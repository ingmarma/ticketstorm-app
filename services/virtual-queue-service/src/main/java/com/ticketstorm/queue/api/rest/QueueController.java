package com.ticketstorm.queue.api.rest;

import com.ticketstorm.queue.application.service.QueueService;
import com.ticketstorm.queue.domain.model.QueueEntry;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
@Timed(value = "queue.controller", description = "Queue controller metrics")
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/join")
    public ResponseEntity<QueueEntry> joinQueue(@RequestBody Map<String, String> request) {
        String eventId = request.get("eventId");
        String userId = request.get("userId");
        QueueEntry entry = queueService.joinQueue(eventId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    @GetMapping("/position/{eventId}")
    public ResponseEntity<Map<String, Object>> getPosition(
            @PathVariable String eventId,
            @RequestParam String userId) {
        long position = queueService.getPosition(eventId, userId);
        long queueSize = queueService.getQueueSize(eventId);
        return ResponseEntity.ok(Map.of(
                "position", position,
                "queueSize", queueSize,
                "eventId", eventId,
                "userId", userId
        ));
    }

    @GetMapping("/size/{eventId}")
    public ResponseEntity<Map<String, Object>> getQueueSize(@PathVariable String eventId) {
        long size = queueService.getQueueSize(eventId);
        return ResponseEntity.ok(Map.of(
                "eventId", eventId,
                "size", size
        ));
    }
}
