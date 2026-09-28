package com.ticketstorm.ai.infrastructure.persistence;

import com.ticketstorm.ai.domain.model.EventEmbedding;
import com.ticketstorm.ai.domain.port.EmbeddingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PgVectorEmbeddingAdapter implements EmbeddingPort {

    private static final Logger log = LoggerFactory.getLogger(PgVectorEmbeddingAdapter.class);

    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    public PgVectorEmbeddingAdapter(EmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    @Override
    public float[] generateEmbedding(String text) {
        log.debug("Generating embedding for text of length {}", text.length());
        return embeddingModel.embed(text);
    }

    @Override
    public List<EventEmbedding> searchSimilar(float[] queryEmbedding, int topK, double threshold) {
        String queryText = java.util.Arrays.toString(queryEmbedding);
        SearchRequest request = SearchRequest.builder()
                .query(queryText)
                .topK(topK)
                .similarityThreshold(threshold)
                .build();

        return vectorStore.similaritySearch(request).stream()
                .map(doc -> {
                    EventEmbedding embedding = new EventEmbedding();
                    embedding.setEventId(doc.getId());
                    embedding.setEventName(doc.getMetadata() != null ?
                            (String) doc.getMetadata().getOrDefault("eventName", "") : "");
                    embedding.setDescription(doc.getText());
                    return embedding;
                })
                .toList();
    }

    @Override
    public void saveEmbedding(EventEmbedding embedding) {
        Document doc = Document.builder()
                .id(embedding.getEventId())
                .text(embedding.getDescription())
                .metadata(Map.of(
                        "eventName", embedding.getEventName(),
                        "category", embedding.getCategory() != null ? embedding.getCategory() : "",
                        "venue", embedding.getVenue() != null ? embedding.getVenue() : "",
                        "city", embedding.getCity() != null ? embedding.getCity() : ""
                ))
                .build();
        vectorStore.add(List.of(doc));
    }

    @Override
    public void saveAllEmbeddings(List<EventEmbedding> embeddings) {
        List<Document> documents = embeddings.stream()
                .map(e -> Document.builder()
                        .id(e.getEventId())
                        .text(e.getDescription())
                        .metadata(Map.of(
                                "eventName", e.getEventName(),
                                "category", e.getCategory() != null ? e.getCategory() : "",
                                "venue", e.getVenue() != null ? e.getVenue() : "",
                                "city", e.getCity() != null ? e.getCity() : ""
                        ))
                        .build())
                .toList();
        vectorStore.add(documents);
        log.info("Saved {} embeddings to vector store", embeddings.size());
    }
}
