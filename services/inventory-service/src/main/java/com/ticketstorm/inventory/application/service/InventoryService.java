package com.ticketstorm.inventory.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketstorm.inventory.domain.model.InventoryEvent;
import com.ticketstorm.inventory.domain.model.SeatSnapshot;
import com.ticketstorm.inventory.domain.port.InventoryEventRepository;
import com.ticketstorm.inventory.domain.port.SeatSnapshotRepository;
import com.ticketstorm.inventory.infrastructure.messaging.KafkaEventProducer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class InventoryService {

    private static final String STOCK_KEY_PREFIX = "inventory:stock:";
    private static final String BLOCKED_KEY_PREFIX = "inventory:blocked:";

    private final InventoryEventRepository eventRepository;
    private final SeatSnapshotRepository snapshotRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaEventProducer kafkaProducer;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    private final DefaultRedisScript<Long> decrementStockScript;
    private final Counter seatsBlockedCounter;
    private final Counter seatsReservedCounter;
    private final Counter seatsReleasedCounter;
    private final Counter overbookingPreventedCounter;

    public InventoryService(
            InventoryEventRepository eventRepository,
            SeatSnapshotRepository snapshotRepository,
            RedisTemplate<String, String> redisTemplate,
            KafkaEventProducer kafkaProducer,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.eventRepository = eventRepository;
        this.snapshotRepository = snapshotRepository;
        this.redisTemplate = redisTemplate;
        this.kafkaProducer = kafkaProducer;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;

        this.decrementStockScript = new DefaultRedisScript<>(ANTI_OVERBOOK_LUA_SCRIPT, Long.class);

        this.seatsBlockedCounter = Counter.builder("inventory.seats.blocked")
                .description("Total seats blocked")
                .register(meterRegistry);
        this.seatsReservedCounter = Counter.builder("inventory.seats.reserved")
                .description("Total seats reserved")
                .register(meterRegistry);
        this.seatsReleasedCounter = Counter.builder("inventory.seats.released")
                .description("Total seats released")
                .register(meterRegistry);
        this.overbookingPreventedCounter = Counter.builder("inventory.overbooking.prevented")
                .description("Overbooking attempts prevented by Redis Lua script")
                .register(meterRegistry);
    }

    private static final String ANTI_OVERBOOK_LUA_SCRIPT =
            """
            local stock_key = KEYS[1]
            local blocked_key = KEYS[2]
            local quantity = tonumber(ARGV[1])
            local current = tonumber(redis.call('GET', stock_key) or '0')
            if current >= quantity then
                redis.call('DECRBY', stock_key, quantity)
                redis.call('INCRBY', blocked_key, quantity)
                return current - quantity
            else
                return -1
            end
            """;

    public long blockSeats(String eventId, String sectionId, int quantity) {
        String stockKey = STOCK_KEY_PREFIX + eventId + ":" + sectionId;
        String blockedKey = BLOCKED_KEY_PREFIX + eventId + ":" + sectionId;

        Long remaining = redisTemplate.execute(
                decrementStockScript,
                List.of(stockKey, blockedKey),
                String.valueOf(quantity));

        if (remaining == null || remaining < 0) {
            overbookingPreventedCounter.increment();
            log.warn("Overbooking prevented: eventId={}, sectionId={}, requested={}",
                    eventId, sectionId, quantity);
            throw new com.ticketstorm.shared.common.exception.SeatNotAvailableException(
                    sectionId, eventId);
        }

        seatsBlockedCounter.increment(quantity);
        log.info("Blocked {} seats for event={} section={}. Remaining stock: {}",
                quantity, eventId, sectionId, remaining);
        return remaining;
    }

    @Transactional
    public InventoryEvent reserveSeat(String eventId, String seatId, String userId, String reservationId) {
        String eventType = "SeatReserved";
        Instant now = Instant.now();

        SeatSnapshot snapshot = snapshotRepository.findByEventIdAndSeatId(eventId, seatId)
                .orElseGet(() -> SeatSnapshot.builder()
                        .eventId(eventId)
                        .seatId(seatId)
                        .sectionId(extractSectionId(seatId))
                        .status(SeatSnapshot.SeatStatus.AVAILABLE)
                        .version(0L)
                        .build());

        if (snapshot.getStatus() != SeatSnapshot.SeatStatus.AVAILABLE
                && snapshot.getStatus() != SeatSnapshot.SeatStatus.BLOCKED) {
            throw new com.ticketstorm.shared.common.exception.SeatNotAvailableException(seatId, eventId);
        }

        snapshot.setStatus(SeatSnapshot.SeatStatus.RESERVED);
        snapshot.setCurrentReservationId(reservationId);
        snapshot.setVersion(snapshot.getVersion() + 1);
        snapshotRepository.save(snapshot);

        InventoryEvent event = InventoryEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .seatId(seatId)
                .sectionId(snapshot.getSectionId())
                .eventType(eventType)
                .userId(userId)
                .price(BigDecimal.ZERO)
                .currency("USD")
                .timestamp(now)
                .payload(buildPayload(eventId, seatId, userId, reservationId))
                .build();
        eventRepository.save(event);

        seatsReservedCounter.increment();
        log.info("Reserved seat={} for event={} user={} reservation={}",
                seatId, eventId, userId, reservationId);

        kafkaProducer.publishSeatReserved(eventId, seatId, snapshot.getSectionId(),
                reservationId, userId, now);
        return event;
    }

    @Transactional
    public void releaseSeat(String reservationId, String reason) {
        Optional<SeatSnapshot> optSnapshot = snapshotRepository.findByCurrentReservationId(reservationId);
        if (optSnapshot.isEmpty()) {
            log.warn("Attempted to release seat for unknown reservation={}", reservationId);
            return;
        }

        SeatSnapshot snapshot = optSnapshot.get();
        String eventType = "SeatReleased";
        Instant now = Instant.now();

        snapshot.setStatus(SeatSnapshot.SeatStatus.RELEASED);
        snapshot.setCurrentReservationId(null);
        snapshot.setVersion(snapshot.getVersion() + 1);
        snapshotRepository.save(snapshot);

        InventoryEvent event = InventoryEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .seatId(snapshot.getSeatId())
                .sectionId(snapshot.getSectionId())
                .eventType(eventType)
                .timestamp(now)
                .payload("{\"reason\":\"" + reason + "\",\"reservationId\":\"" + reservationId + "\"}")
                .build();
        eventRepository.save(event);

        String stockKey = STOCK_KEY_PREFIX + snapshot.getEventId() + ":" + snapshot.getSectionId();
        redisTemplate.opsForValue().increment(stockKey);

        seatsReleasedCounter.increment();
        log.info("Released seat={} reservation={} reason={}", snapshot.getSeatId(), reservationId, reason);

        kafkaProducer.publishSeatReleased(snapshot.getEventId(), snapshot.getSeatId(),
                snapshot.getSectionId(), reason, now);
    }

    @Transactional
    public void confirmSeat(String reservationId, String paymentId) {
        Optional<SeatSnapshot> optSnapshot = snapshotRepository.findByCurrentReservationId(reservationId);
        if (optSnapshot.isEmpty()) {
            log.warn("Attempted to confirm seat for unknown reservation={}", reservationId);
            return;
        }

        SeatSnapshot snapshot = optSnapshot.get();
        String eventType = "SeatConfirmed";
        Instant now = Instant.now();

        snapshot.setStatus(SeatSnapshot.SeatStatus.CONFIRMED);
        snapshot.setVersion(snapshot.getVersion() + 1);
        snapshotRepository.save(snapshot);

        InventoryEvent event = InventoryEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .seatId(snapshot.getSeatId())
                .sectionId(snapshot.getSectionId())
                .eventType(eventType)
                .timestamp(now)
                .payload("{\"paymentId\":\"" + paymentId + "\",\"reservationId\":\"" + reservationId + "\"}")
                .build();
        eventRepository.save(event);

        log.info("Confirmed seat={} paymentId={}", snapshot.getSeatId(), paymentId);

        kafkaProducer.publishSeatConfirmed(snapshot.getEventId(), snapshot.getSeatId(),
                snapshot.getSectionId(), snapshot.getCurrentReservationId(),
                paymentId, now);
    }

    public Map<String, Long> getAvailableSeats(String eventId, String sectionId) {
        String stockKey = STOCK_KEY_PREFIX + eventId + ":" + sectionId;
        String val = redisTemplate.opsForValue().get(stockKey);
        long available = val != null ? Long.parseLong(val) : 0L;

        Map<String, Long> result = new LinkedHashMap<>();
        result.put(sectionId, available);
        return result;
    }

    public Map<String, Map<String, Long>> getAllAvailability(String eventId) {
        List<SeatSnapshot> snapshots = snapshotRepository.findByEventId(eventId);
        Map<String, Long> sectionCounts = new TreeMap<>();

        for (SeatSnapshot s : snapshots) {
            if (s.getStatus() == SeatSnapshot.SeatStatus.AVAILABLE) {
                sectionCounts.merge(s.getSectionId(), 1L, Long::sum);
            }
        }

        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        result.put("sections", sectionCounts);
        return result;
    }

    public void initializeStock(String eventId, String sectionId, long quantity) {
        String stockKey = STOCK_KEY_PREFIX + eventId + ":" + sectionId;
        redisTemplate.opsForValue().set(stockKey, String.valueOf(quantity));
        redisTemplate.expire(stockKey, 24, TimeUnit.HOURS);
        log.info("Initialized stock: event={} section={} quantity={}", eventId, sectionId, quantity);
    }

    public Map<String, String> buildSeatSnapshot(String eventId, String seatId) {
        Map<String, String> data = new LinkedHashMap<>();
        Optional<SeatSnapshot> opt = snapshotRepository.findByEventIdAndSeatId(eventId, seatId);
        if (opt.isPresent()) {
            SeatSnapshot s = opt.get();
            data.put("seatId", s.getSeatId());
            data.put("eventId", s.getEventId());
            data.put("sectionId", s.getSectionId());
            data.put("status", s.getStatus().name());
            data.put("version", String.valueOf(s.getVersion()));
            if (s.getCurrentReservationId() != null) {
                data.put("reservationId", s.getCurrentReservationId());
            }
        }
        return data;
    }

    private String extractSectionId(String seatId) {
        if (seatId.contains(":")) {
            return seatId.split(":")[1];
        }
        return seatId.substring(0, Math.min(4, seatId.length()));
    }

    private String buildPayload(String eventId, String seatId, String userId, String reservationId) {
        try {
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("eventId", eventId);
            payload.put("seatId", seatId);
            payload.put("userId", userId);
            payload.put("reservationId", reservationId);
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
