package com.ticketstorm.ai.domain.port;

import com.ticketstorm.ai.domain.model.EventEmbedding;

import java.util.List;

public interface EmbeddingPort {
    float[] generateEmbedding(String text);
    List<EventEmbedding> searchSimilar(float[] queryEmbedding, int topK, double threshold);
    void saveEmbedding(EventEmbedding embedding);
    void saveAllEmbeddings(List<EventEmbedding> embeddings);
}
