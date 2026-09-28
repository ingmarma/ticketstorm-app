package com.ticketstorm.ai.application.service;

import com.ticketstorm.ai.application.dto.ChatRequest;
import com.ticketstorm.ai.application.dto.ChatResponse;
import com.ticketstorm.ai.application.dto.Event;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    public static final String SYSTEM_PROMPT = """
            Eres el asistente de TicketStorm, una plataforma de venta de entradas en Paraguay. \
            Respondes en español. Cuando el usuario pregunte por eventos, busca en la base de datos \
            y recomienda eventos relevantes. Incluye precios en Guaraníes (₲). Sé amigable y conciso.
            """;

    private static final int MAX_CONTEXT_EVENTS = 5;
    private static final double SIMILARITY_THRESHOLD = 0.1;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final EventIndexService eventIndexService;
    private final Counter chatRequestsCounter;

    public ChatService(ChatModel chatModel,
                       VectorStore vectorStore,
                       EventIndexService eventIndexService,
                       MeterRegistry meterRegistry) {
        var chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.vectorStore = vectorStore;
        this.eventIndexService = eventIndexService;
        this.chatRequestsCounter = Counter.builder("ai.chat.requests.total")
                .description("Total chat requests")
                .register(meterRegistry);
    }

    public ChatResponse chat(ChatRequest request) {
        chatRequestsCounter.increment();

        String sessionId = request.sessionId() == null || request.sessionId().isBlank()
                ? UUID.randomUUID().toString()
                : request.sessionId();
        String message = request.message() == null ? "" : request.message().trim();

        log.info("Chat request: sessionId={}, message='{}'", sessionId, message);

        List<Event> events = searchEvents(message);
        String systemMessage = SYSTEM_PROMPT + buildContext(events);

        String response;
        try {
            response = chatClient.prompt()
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                    .system(systemMessage)
                    .user(message)
                    .call()
                    .content();
        } catch (Exception ex) {
            log.error("Chat model unavailable for session {}: {}", sessionId, ex.getMessage());
            response = fallbackResponse(events);
        }

        if (response == null || response.isBlank()) {
            response = fallbackResponse(events);
        }

        log.info("Chat response generated for session {} ({} recommended events)", sessionId, events.size());
        return new ChatResponse(response, events, sessionId);
    }

    public Flux<String> chatStream(String sessionId, String message) {
        chatRequestsCounter.increment();
        log.info("Chat stream request: sessionId={}, message='{}'", sessionId, message);

        String conversationId = sessionId == null || sessionId.isBlank()
                ? UUID.randomUUID().toString()
                : sessionId;
        String systemMessage = SYSTEM_PROMPT + buildContext(searchEvents(message));

        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .system(systemMessage)
                .user(message)
                .stream()
                .content();
    }

    private List<Event> searchEvents(String message) {
        if (message == null || message.isBlank()) {
            return List.of();
        }
        try {
            List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(message)
                    .topK(MAX_CONTEXT_EVENTS)
                    .similarityThreshold(SIMILARITY_THRESHOLD)
                    .build());

            if (documents == null || documents.isEmpty()) {
                return List.of();
            }
            return documents.stream()
                    .map(eventIndexService::toEvent)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception ex) {
            log.warn("Similarity search failed, answering without event context: {}", ex.getMessage());
            return List.of();
        }
    }

    private String buildContext(List<Event> events) {
        if (events.isEmpty()) {
            return "\n\nNo hay eventos indexados en este momento. Avisa al usuario y sugiérele intentar más tarde.";
        }
        StringBuilder context = new StringBuilder("\n\nEventos relevantes encontrados en la base de datos:\n");
        for (Event event : events) {
            context.append("- ").append(event.name())
                    .append(" | ").append(event.venue()).append(", ").append(event.city())
                    .append(" | ").append(event.category())
                    .append(" | ").append(event.eventDate())
                    .append(" | desde ₲").append(event.minPrice()).append(" ").append(event.currency())
                    .append('\n');
        }
        context.append("Recomienda estos eventos si responden a la pregunta. Si ninguno encaja, dilo con amabilidad.");
        return context.toString();
    }

    private String fallbackResponse(List<Event> events) {
        if (events.isEmpty()) {
            return "El asistente de IA no está disponible en este momento. "
                    + "Intenta de nuevo en unos segundos o usa el buscador de eventos.";
        }
        StringBuilder message = new StringBuilder(
                "El asistente de IA no está disponible en este momento, pero encontré estos eventos para ti:\n");
        for (Event event : events) {
            message.append("- ").append(event.name())
                    .append(" (").append(event.venue()).append(", ").append(event.city())
                    .append(") desde ₲").append(event.minPrice()).append(" ")
                    .append(event.currency()).append('\n');
        }
        return message.toString();
    }
}
