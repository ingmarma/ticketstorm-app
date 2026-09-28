package com.ticketstorm.ai.application.service;

import com.ticketstorm.ai.domain.model.EventEmbedding;
import com.ticketstorm.ai.domain.port.EmbeddingPort;
import com.ticketstorm.shared.common.dto.EventResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SemanticSearchService {

    private static final Logger log = LoggerFactory.getLogger(SemanticSearchService.class);

    private final VectorStore vectorStore;
    private final EmbeddingPort embeddingPort;
    private final Counter searchRequestsCounter;
    private final Timer searchLatencyTimer;

    public SemanticSearchService(VectorStore vectorStore, EmbeddingPort embeddingPort,
                                  MeterRegistry meterRegistry) {
        this.vectorStore = vectorStore;
        this.embeddingPort = embeddingPort;
        this.searchRequestsCounter = Counter.builder("ai.search.requests.total")
                .description("Total semantic search requests")
                .register(meterRegistry);
        this.searchLatencyTimer = Timer.builder("ai.search.latency.seconds")
                .description("Semantic search latency")
                .register(meterRegistry);
    }

    public List<EventResponse.SearchResult> search(String query, int topK, double threshold) {
        return searchLatencyTimer.record(() -> {
            searchRequestsCounter.increment();
            log.info("Performing semantic search: query='{}', topK={}, threshold={}", query, topK, threshold);

            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(threshold)
                    .build();

            List<Document> docs = vectorStore.similaritySearch(request);

            List<EventResponse.SearchResult> results = docs.stream()
                    .map(doc -> {
                        String eventId = doc.getId();
                        String name = doc.getMetadata() != null ?
                                (String) doc.getMetadata().getOrDefault("eventName", "Unknown") : "Unknown";
                        String category = doc.getMetadata() != null ?
                                (String) doc.getMetadata().getOrDefault("category", "") : "";
                        double score = doc.getMetadata() != null ?
                                (double) doc.getMetadata().getOrDefault("score", 0.0) : 0.0;

                        return new EventResponse.SearchResult(
                                new EventResponse(eventId, name, doc.getText(), category,
                                        null, null, null, null, null, null, null, 0, 0, null, null, null),
                                score,
                                "Semantic similarity match"
                        );
                    })
                    .toList();

            log.info("Semantic search returned {} results", results.size());
            return results;
        });
    }
}
