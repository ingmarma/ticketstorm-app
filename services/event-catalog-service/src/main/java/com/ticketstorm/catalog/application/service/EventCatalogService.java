package com.ticketstorm.catalog.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketstorm.catalog.domain.model.Event;
import com.ticketstorm.catalog.domain.port.EventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class EventCatalogService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final String CACHE_PREFIX = "event:";

    private final EventRepository eventRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public EventCatalogService(EventRepository eventRepository,
                                RedisTemplate<String, Object> redisTemplate,
                                ObjectMapper objectMapper,
                                MeterRegistry meterRegistry) {
        this.eventRepository = eventRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public Optional<Event> getEvent(UUID id) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            String cacheKey = CACHE_PREFIX + id;
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Cache hit for event {}", id);
                meterRegistry.counter("catalog.cache.hit").increment();
                Event event = objectMapper.convertValue(cached, new TypeReference<>() {});
                return Optional.of(event);
            }

            meterRegistry.counter("catalog.cache.miss").increment();
            Optional<Event> event = eventRepository.findByIdWithSections(id);
            event.ifPresent(e -> {
                redisTemplate.opsForValue().set(cacheKey, e, CACHE_TTL);
                log.debug("Cached event {}", id);
            });
            return event;
        } finally {
            sample.stop(meterRegistry.timer("catalog.getEvent"));
        }
    }

    @Transactional(readOnly = true)
    public List<Event> listEvents(String category, String city, Instant dateFrom, Instant dateTo) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            if (category != null && city != null) {
                return eventRepository.findByCategoryAndCity(category, city);
            }
            if (category != null) {
                return eventRepository.findByCategory(category);
            }
            if (city != null) {
                return eventRepository.findByCity(city);
            }
            if (dateFrom != null && dateTo != null) {
                return eventRepository.findByEventDateBetween(dateFrom, dateTo);
            }
            return eventRepository.findAll();
        } finally {
            sample.stop(meterRegistry.timer("catalog.listEvents"));
        }
    }

    @Transactional(readOnly = true)
    public List<Event> searchEvents(String query) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            meterRegistry.counter("catalog.search").increment();
            return eventRepository.searchByFullText(query);
        } finally {
            sample.stop(meterRegistry.timer("catalog.searchEvents"));
        }
    }

    @Transactional
    public Event createEvent(Event event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            event.setCreatedAt(Instant.now());
            event.setUpdatedAt(Instant.now());
            event.setAvailableSeats(event.getTotalSeats());
            if (event.getCurrency() == null) {
                event.setCurrency("PYG");
            }
            Event saved = eventRepository.save(event);
            log.info("Created event {} - {}", saved.getId(), saved.getName());
            meterRegistry.counter("catalog.events.created").increment();
            return saved;
        } finally {
            sample.stop(meterRegistry.timer("catalog.createEvent"));
        }
    }

    @Transactional
    public Event updateEvent(UUID id, Event update) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Event existing = eventRepository.findById(id)
                    .orElseThrow(() -> new com.ticketstorm.shared.common.exception.EventNotFoundException(id.toString()));

            existing.setName(update.getName());
            existing.setDescription(update.getDescription());
            existing.setCategory(update.getCategory());
            existing.setVenue(update.getVenue());
            existing.setCity(update.getCity());
            existing.setEventDate(update.getEventDate());
            existing.setSaleStart(update.getSaleStart());
            existing.setSaleEnd(update.getSaleEnd());
            existing.setMinPrice(update.getMinPrice());
            existing.setCurrency(update.getCurrency() != null ? update.getCurrency() : "PYG");
            existing.setTotalSeats(update.getTotalSeats());
            existing.setImageUrl(update.getImageUrl());
            existing.setUpdatedAt(Instant.now());

            Event saved = eventRepository.save(existing);
            redisTemplate.delete(CACHE_PREFIX + id);
            log.info("Updated event {}", id);
            meterRegistry.counter("catalog.events.updated").increment();
            return saved;
        } finally {
            sample.stop(meterRegistry.timer("catalog.updateEvent"));
        }
    }
}
