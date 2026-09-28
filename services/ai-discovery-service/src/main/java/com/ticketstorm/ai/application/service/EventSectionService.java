package com.ticketstorm.ai.application.service;

import com.ticketstorm.ai.application.dto.TicketSectionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EventSectionService {

    private static final Logger log = LoggerFactory.getLogger(EventSectionService.class);
    private static final String SECTIONS_PATH = "/api/v1/events/{id}/sections";
    private static final long CACHE_TTL_MS = 30_000L;

    private record CachedSections(List<TicketSectionDto> sections, long loadedAt) {}

    private final RestClient restClient;
    private final Map<String, CachedSections> cache = new ConcurrentHashMap<>();

    public EventSectionService(
            @Value("${event-catalog.base-url}") String baseUrl,
            @Value("${event-catalog.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${event-catalog.read-timeout-ms:15000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public List<TicketSectionDto> sections(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return List.of();
        }
        long now = System.currentTimeMillis();
        CachedSections cached = cache.get(eventId);
        if (cached != null && now - cached.loadedAt() < CACHE_TTL_MS) {
            return cached.sections();
        }
        try {
            List<TicketSectionDto> sections = restClient.get()
                    .uri(SECTIONS_PATH, eventId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<TicketSectionDto>>() {});
            List<TicketSectionDto> result = sections == null ? List.of() : List.copyOf(sections);
            cache.put(eventId, new CachedSections(result, now));
            return result;
        } catch (Exception ex) {
            log.warn("Could not load sections for event {}: {}", eventId, ex.getMessage());
            return cached == null ? List.of() : cached.sections();
        }
    }
}
