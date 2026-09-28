package com.ticketstorm.ai.application.service;

import com.ticketstorm.ai.application.dto.EventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class EventIndexService {

    private static final Logger log = LoggerFactory.getLogger(EventIndexService.class);

    private static final String EVENTS_PATH = "/api/v1/events";

    private final RestClient restClient;
    private final VectorStore vectorStore;
    private final int maxIndexAttempts;
    private final long indexRetryDelayMs;
    private final Map<String, EventDto> eventRegistry = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "event-indexer");
        thread.setDaemon(true);
        return thread;
    });
    private final Object indexLock = new Object();

    private volatile boolean indexed;
    private volatile boolean startupIndexing;

    public EventIndexService(
            @Value("${event-catalog.base-url}") String baseUrl,
            @Value("${event-catalog.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${event-catalog.read-timeout-ms:15000}") int readTimeoutMs,
            @Value("${event-catalog.index-max-attempts:60}") int maxIndexAttempts,
            @Value("${event-catalog.index-retry-delay-ms:10000}") long indexRetryDelayMs,
            VectorStore vectorStore) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.vectorStore = vectorStore;
        this.maxIndexAttempts = maxIndexAttempts;
        this.indexRetryDelayMs = indexRetryDelayMs;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startIndexing() {
        executor.submit(() -> indexWithRetries());
    }

    @Scheduled(
            fixedDelayString = "${event-catalog.reindex-interval-ms:600000}",
            initialDelayString = "${event-catalog.reindex-interval-ms:600000}")
    public void scheduledReindex() {
        if (startupIndexing) {
            return;
        }
        try {
            index();
        } catch (Exception ex) {
            log.warn("Scheduled event re-index failed: {}", ex.getMessage());
        }
    }

    public void indexWithRetries() {
        startupIndexing = true;
        try {
            for (int attempt = 1; attempt <= maxIndexAttempts; attempt++) {
                try {
                    index();
                    return;
                } catch (Exception ex) {
                    log.warn("Event indexing attempt {}/{} failed: {}", attempt, maxIndexAttempts, ex.getMessage());
                    if (attempt < maxIndexAttempts) {
                        try {
                            Thread.sleep(indexRetryDelayMs);
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
            log.error("Could not index events after {} attempts. Chat answers will have no event context.", maxIndexAttempts);
        } finally {
            startupIndexing = false;
        }
    }

    public void index() {
        List<EventDto> events = restClient.get()
                .uri(EVENTS_PATH)
                .retrieve()
                .body(new ParameterizedTypeReference<List<EventDto>>() {});

        synchronized (indexLock) {
            List<Document> documents = new ArrayList<>();
            for (EventDto event : events == null ? List.<EventDto>of() : events) {
                if (event == null || event.id() == null) {
                    continue;
                }
                eventRegistry.put(event.id(), event);
                documents.add(toDocument(event));
            }
            if (documents.isEmpty()) {
                log.info("Event catalog returned no events, vector store left untouched");
                indexed = true;
                return;
            }
            vectorStore.add(documents);
            indexed = true;
            log.info("Indexed {} events into the vector store ({} known events)", documents.size(), eventRegistry.size());
        }
    }

    public boolean isIndexed() {
        return indexed;
    }

    public Optional<EventDto> findById(String eventId) {
        if (eventId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(eventRegistry.get(eventId));
    }

    public List<EventDto> all() {
        return List.copyOf(eventRegistry.values());
    }

    public int size() {
        return eventRegistry.size();
    }

    public EventDto toEvent(Document document) {
        EventDto cached = eventRegistry.get(document.getId());
        if (cached != null) {
            return cached;
        }
        Map<String, Object> metadata = document.getMetadata() == null ? Map.of() : document.getMetadata();
        BigDecimal minPrice = toBigDecimal(metadata.get("minPrice"));
        return new EventDto(
                document.getId(),
                text(metadata.get("name")),
                document.getText(),
                text(metadata.get("venue")),
                text(metadata.get("city")),
                text(metadata.get("category")),
                parseInstant(text(metadata.get("eventDate"))),
                minPrice,
                text(metadata.get("currency")),
                null,
                0,
                0,
                null,
                null
        );
    }

    private Document toDocument(EventDto event) {
        return Document.builder()
                .id(event.id())
                .text(buildDocumentText(event))
                .metadata(Map.of(
                        "id", event.id(),
                        "name", event.name() == null ? "" : event.name(),
                        "venue", event.venue() == null ? "" : event.venue(),
                        "city", event.city() == null ? "" : event.city(),
                        "category", event.category() == null ? "" : event.category(),
                        "eventDate", event.eventDate() == null ? "" : event.eventDate().toString(),
                        "minPrice", event.minPrice() == null ? 0d : event.minPrice().doubleValue(),
                        "currency", event.currency() == null ? "" : event.currency()
                ))
                .build();
    }

    private String buildDocumentText(EventDto event) {
        StringBuilder text = new StringBuilder(event.name() == null ? "" : event.name());
        if (event.description() != null && !event.description().isBlank()) {
            text.append(". ").append(event.description());
        }
        text.append("\nCategoría: ").append(event.category() == null ? "N/D" : event.category());
        text.append("\nSede: ").append(event.venue() == null ? "N/D" : event.venue());
        text.append(", ").append(event.city() == null ? "N/D" : event.city());
        text.append("\nFecha: ").append(event.eventDate() == null ? "N/D" : event.eventDate());
        text.append("\nPrecio desde: ₲").append(event.minPrice() == null ? "0" : event.minPrice());
        text.append(" ").append(event.currency() == null ? "PYG" : event.currency());
        return text.toString();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
