package com.ticketstorm.ai.application.service;

import com.ticketstorm.ai.application.dto.ChatAction;
import com.ticketstorm.ai.application.dto.ChatRequest;
import com.ticketstorm.ai.application.dto.ChatResponse;
import com.ticketstorm.ai.application.dto.EventDto;
import com.ticketstorm.ai.application.dto.TicketSectionDto;
import com.ticketstorm.ai.application.service.ChatIntentResolver.Intent;
import com.ticketstorm.ai.application.service.ChatIntentResolver.ResolvedIntent;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    public static final String SYSTEM_PROMPT = """
            Eres el asistente de TicketStorm, una plataforma de venta de entradas en Paraguay. \
            Respondes en español. Cuando el usuario pregunte por eventos, busca en la base de datos \
            y recomienda eventos relevantes. Incluye precios en Guaraníes (₲). Sé amigable y conciso.

            Flujo de conversación:
            1. Búsqueda: recomienda solo los eventos del contexto. Si ninguno encaja, dilo con amabilidad.
            2. Secciones y disponibilidad: cuando pregunte por entradas, secciones, precios o \
            disponibilidad, responde solo con las secciones del evento indicado en el contexto: \
            nombre, precio y entradas disponibles sobre el total. No inventes datos.
            3. Compra: cuando el usuario quiera comprar, confirma evento, sección, cantidad y total \
            (precio por cantidad) e invítalo a completar la compra con el botón del chat.
            4. Si falta el evento o la sección, pide que te indique cuál quiere.
            Nunca inventes eventos, precios ni disponibilidad: usa únicamente el contexto recibido.
            """;

    private static final int MAX_CONTEXT_EVENTS = 5;
    private static final double SIMILARITY_THRESHOLD = 0.1;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final EventIndexService eventIndexService;
    private final ChatIntentResolver intentResolver;
    private final Counter chatRequestsCounter;

    public ChatService(ChatModel chatModel,
                       VectorStore vectorStore,
                       EventIndexService eventIndexService,
                       ChatIntentResolver intentResolver,
                       MeterRegistry meterRegistry) {
        var chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.vectorStore = vectorStore;
        this.eventIndexService = eventIndexService;
        this.intentResolver = intentResolver;
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

        List<EventDto> events = searchEvents(message);
        ResolvedIntent resolved = intentResolver.resolve(sessionId, message, events);
        events = withFocusFirst(events, resolved.focusEvent());

        String systemMessage = SYSTEM_PROMPT + buildContext(events, resolved);

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
            response = fallbackResponse(events, resolved);
        }

        if (response == null || response.isBlank()) {
            response = fallbackResponse(events, resolved);
        }

        ChatAction action = toAction(resolved);
        log.info("Chat response generated for session {} (intent={}, events={}, sections={}, action={})",
                sessionId, resolved.intent(), events.size(), resolved.sections().size(),
                action == null ? "none" : action.type());
        return new ChatResponse(response, events, resolved.sections(), action, sessionId);
    }

    public Flux<String> chatStream(String sessionId, String message) {
        chatRequestsCounter.increment();
        log.info("Chat stream request: sessionId={}, message='{}'", sessionId, message);

        String conversationId = sessionId == null || sessionId.isBlank()
                ? UUID.randomUUID().toString()
                : sessionId;
        String plainMessage = message == null ? "" : message.trim();
        List<EventDto> events = searchEvents(plainMessage);
        ResolvedIntent resolved = intentResolver.resolve(conversationId, plainMessage, events);
        events = withFocusFirst(events, resolved.focusEvent());

        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .system(SYSTEM_PROMPT + buildContext(events, resolved))
                .user(plainMessage)
                .stream()
                .content();
    }

    private ChatAction toAction(ResolvedIntent resolved) {
        if (resolved.focusEvent() == null || resolved.intent() == Intent.SEARCH) {
            return null;
        }
        String eventId = resolved.focusEvent().id();
        return switch (resolved.intent()) {
            case VIEW_EVENT -> new ChatAction(ChatAction.VIEW_EVENT, eventId, null, null);
            case VIEW_SECTIONS -> new ChatAction(ChatAction.VIEW_SECTIONS, eventId,
                    resolved.section() == null ? null : resolved.section().id(), null);
            case BUY -> new ChatAction(ChatAction.BUY, eventId,
                    resolved.section() == null ? null : resolved.section().id(),
                    resolved.quantity());
            default -> null;
        };
    }

    private List<EventDto> withFocusFirst(List<EventDto> events, EventDto focus) {
        if (focus == null) {
            return events;
        }
        List<EventDto> ordered = new ArrayList<>();
        ordered.add(focus);
        for (EventDto event : events) {
            if (event != null && !focus.id().equals(event.id())) {
                ordered.add(event);
            }
        }
        return ordered;
    }

    private List<EventDto> searchEvents(String message) {
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

    private String buildContext(List<EventDto> events, ResolvedIntent resolved) {
        StringBuilder context = new StringBuilder();
        if (events.isEmpty()) {
            context.append("\n\nNo hay eventos indexados en este momento. "
                    + "Avisa al usuario y sugiérele intentar más tarde.");
        } else {
            context.append("\n\nEventos relevantes encontrados en la base de datos:\n");
            for (EventDto event : events) {
                context.append("- ").append(event.name())
                        .append(" | ").append(event.venue()).append(", ").append(event.city())
                        .append(" | ").append(event.category())
                        .append(" | ").append(event.eventDate())
                        .append(" | desde ₲").append(event.minPrice()).append(" ").append(event.currency())
                        .append('\n');
            }
        }

        if (!resolved.sections().isEmpty() && resolved.focusEvent() != null) {
            context.append("\nSecciones de ").append(resolved.focusEvent().name()).append(":\n");
            for (TicketSectionDto section : resolved.sections()) {
                context.append("- ").append(section.name())
                        .append(" | ₲").append(section.price()).append(" ").append(section.currency())
                        .append(" | ").append(section.availableCapacity())
                        .append(" de ").append(section.totalCapacity()).append(" entradas disponibles")
                        .append('\n');
            }
        }

        switch (resolved.intent()) {
            case BUY -> context.append("\nEl usuario quiere comprar: evento=")
                    .append(resolved.focusEvent().name())
                    .append(", sección=").append(resolved.section().name())
                    .append(", cantidad=").append(resolved.quantity())
                    .append(", total=₲").append(resolved.section().price().longValue() * resolved.quantity())
                    .append(". Confirma esos datos y menciona el botón para completar la compra.");
            case VIEW_SECTIONS -> context.append("\nEl usuario pregunta por las entradas de ")
                    .append(resolved.focusEvent().name())
                    .append(". Responde solo con las secciones listadas, su precio y disponibilidad.");
            case VIEW_EVENT -> context.append("\nEl usuario quiere información de ")
                    .append(resolved.focusEvent().name())
                    .append(". Enfoca tu respuesta en ese evento.");
            default -> {
                if (!events.isEmpty()) {
                    context.append("\nRecomienda estos eventos si responden a la pregunta. "
                            + "Si ninguno encaja, dilo con amabilidad.");
                }
            }
        }
        return context.toString();
    }

    private String fallbackResponse(List<EventDto> events, ResolvedIntent resolved) {
        if (resolved.intent() == Intent.BUY && resolved.section() != null && resolved.focusEvent() != null) {
            long total = resolved.section().price().longValue() * resolved.quantity();
            return "Tu selección quedó registrada: " + resolved.quantity() + " × "
                    + resolved.section().name() + " para " + resolved.focusEvent().name()
                    + " (total ₲" + total + "). Pulsa el botón de compra para continuar.";
        }
        if (!resolved.sections().isEmpty() && resolved.focusEvent() != null) {
            StringBuilder message = new StringBuilder("Entradas de ")
                    .append(resolved.focusEvent().name()).append(":\n");
            for (TicketSectionDto section : resolved.sections()) {
                message.append("- ").append(section.name())
                        .append(" desde ₲").append(section.price()).append(" ")
                        .append(section.currency())
                        .append(" (").append(section.availableCapacity())
                        .append("/").append(section.totalCapacity()).append(" disponibles)\n");
            }
            return message.toString();
        }
        if (events.isEmpty()) {
            return "El asistente de IA no está disponible en este momento. "
                    + "Intenta de nuevo en unos segundos o usa el buscador de eventos.";
        }
        StringBuilder message = new StringBuilder(
                "El asistente de IA no está disponible en este momento, pero encontré estos eventos para ti:\n");
        for (EventDto event : events) {
            message.append("- ").append(event.name())
                    .append(" (").append(event.venue()).append(", ").append(event.city())
                    .append(") desde ₲").append(event.minPrice()).append(" ")
                    .append(event.currency()).append('\n');
        }
        return message.toString();
    }
}
