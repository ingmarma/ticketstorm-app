package com.ticketstorm.queue.api.websocket;

import com.ticketstorm.queue.application.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final QueueService queueService;

    private final Map<String, String> userSubscriptions = new ConcurrentHashMap<>();

    public void sendPositionUpdate(String userId, String eventId) {
        long position = queueService.getPosition(eventId, userId);
        long queueSize = queueService.getQueueSize(eventId);

        Map<String, Object> update = Map.of(
                "userId", userId,
                "eventId", eventId,
                "position", position,
                "queueSize", queueSize,
                "status", position >= 0 ? "WAITING" : "NOT_IN_QUEUE"
        );

        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/position",
                update
        );

        log.debug("Sent position update to user {}: position {}/{}", userId, position, queueSize);
    }

    public void broadcastQueueUpdate(String eventId) {
        Map<String, Object> update = Map.of(
                "eventId", eventId,
                "queueSize", queueService.getQueueSize(eventId)
        );

        messagingTemplate.convertAndSend("/topic/queue/" + eventId, (Object) update);
    }
}
