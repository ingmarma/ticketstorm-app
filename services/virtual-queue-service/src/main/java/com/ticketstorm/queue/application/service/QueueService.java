package com.ticketstorm.queue.application.service;

import com.ticketstorm.queue.domain.model.QueueEntry;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.MINUTES;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final StringRedisTemplate redisTemplate;

    @Value("${queue.jwt.secret:ticketstorm-queue-jwt-secret-key-that-is-long-enough-for-hs512-algorithm!}")
    private String jwtSecret;

    private static final String QUEUE_KEY_PREFIX = "queue:event:";
    private static final long TURN_TOKEN_TTL_MINUTES = 10;

    public QueueEntry joinQueue(String eventId, String userId) {
        String key = QUEUE_KEY_PREFIX + eventId;
        Long position = redisTemplate.opsForZSet().add(key, userId, System.currentTimeMillis()) ? 
                redisTemplate.opsForZSet().zCard(key) : null;

        if (position == null) {
            throw new IllegalStateException("Failed to join queue for event: " + eventId);
        }

        log.info("User {} joined queue for event {} at position {}", userId, eventId, position);

        return new QueueEntry(
                userId,
                eventId,
                position.intValue(),
                Instant.now(),
                QueueEntry.QueueStatus.WAITING
        );
    }

    public long getPosition(String eventId, String userId) {
        String key = QUEUE_KEY_PREFIX + eventId;
        Long rank = redisTemplate.opsForZSet().rank(key, userId);
        return rank != null ? rank : -1;
    }

    public long getQueueSize(String eventId) {
        String key = QUEUE_KEY_PREFIX + eventId;
        Long size = redisTemplate.opsForZSet().zCard(key);
        return size != null ? size : 0;
    }

    public String grantTurn(String userId, String eventId) {
        long now = System.currentTimeMillis();
        SecretKey key = Keys.hmacShaKeyFor(Base64.getEncoder().encode(jwtSecret.getBytes(StandardCharsets.UTF_8)));

        String token = Jwts.builder()
                .subject(userId)
                .claim("eventId", eventId)
                .claim("type", "queue-turn")
                .issuedAt(new java.util.Date(now))
                .expiration(new java.util.Date(now + TimeUnit.MINUTES.toMillis(TURN_TOKEN_TTL_MINUTES)))
                .signWith(key)
                .compact();

        String queueKey = QUEUE_KEY_PREFIX + eventId;
        redisTemplate.opsForZSet().remove(queueKey, userId);

        log.info("Granted turn token to user {} for event {}", userId, eventId);
        return token;
    }
}
