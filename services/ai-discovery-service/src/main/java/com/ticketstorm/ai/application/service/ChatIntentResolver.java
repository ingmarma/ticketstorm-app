package com.ticketstorm.ai.application.service;

import com.ticketstorm.ai.application.dto.EventDto;
import com.ticketstorm.ai.application.dto.TicketSectionDto;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatIntentResolver {

    public enum Intent {
        SEARCH, VIEW_EVENT, VIEW_SECTIONS, BUY
    }

    public record ResolvedIntent(
            Intent intent,
            EventDto focusEvent,
            List<TicketSectionDto> sections,
            TicketSectionDto section,
            int quantity
    ) {
        static ResolvedIntent none() {
            return new ResolvedIntent(Intent.SEARCH, null, List.of(), null, 0);
        }
    }

    private static final Pattern NUMBER = Pattern.compile("\\b\\d{1,2}\\b");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9ñ]+");

    private static final Set<String> PURCHASE_WORDS = Set.of(
            "comprar", "compra", "compras", "comprando", "comprare", "comprame", "comprale",
            "llevar", "llevarme", "llevame", "llevo",
            "reservar", "reserva", "reservame", "reservale",
            "adquirir", "adquiero", "pagar", "pagame", "pague");

    private static final Set<String> TICKET_WORDS = Set.of(
            "entrada", "entradas", "boleto", "boletos", "localidad", "localidades",
            "ticket", "tickets");

    private static final Set<String> SECTION_WORDS = Set.of(
            "entrada", "entradas", "seccion", "secciones", "localidad", "localidades", "categoria", "categorias",
            "precio", "precios", "disponibilidad", "disponible", "disponibles", "quedan",
            "aforo", "platea", "plateas", "vip", "palco", "palcos", "general", "grada",
            "gradas", "sector", "sectores", "anfiteatro");

    private static final List<String> SECTION_PHRASES = List.of(
            "cuanto sale", "cuanto cuesta", "cuanto vale", "cuanto cuestan",
            "cuantas quedan", "que entradas hay", "que secciones hay");

    private static final Set<String> DETAIL_WORDS = Set.of(
            "detalle", "detalles", "informacion", "info", "descripcion",
            "resumen", "horario", "horarios", "direccion", "cuentame", "hablame");

    private static final List<String> DETAIL_PHRASES = List.of(
            "cuando es", "donde es", "a que hora", "sobre el evento", "del evento",
            "del concierto", "del partido");

    private static final Set<String> STOPWORDS = Set.of(
            "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "al", "a",
            "o", "y", "e", "en", "con", "por", "para", "sin", "sobre", "entre", "que",
            "cual", "cuales", "como", "donde", "cuando", "quien", "quienes", "es", "son",
            "ser", "estar", "estan", "hay", "tengo", "quiero", "queremos", "puedo", "podemos",
            "necesito", "busco", "mas", "menos", "muy", "hola", "gracias", "favor", "ver",
            "ir", "voy", "evento", "eventos", "entradas", "entrada", "comprar",
            "compra", "precio", "precios", "seccion", "secciones", "disponibilidad",
            "disponible", "disponibles", "esta", "este", "esto", "ese", "esa", "aqui",
            "alli", "tambien", "solo", "cuanto", "cuanta", "cuantos", "cuantas", "sale",
            "salen", "cuesta", "cuestan", "vale", "valen", "quedar", "quedan", "pueden",
            "dias", "dia", "mes", "año", "anos", "hoy", "ayer", "manana");

    private static final Map<String, Integer> WORD_NUMBERS = Map.ofEntries(
            Map.entry("un", 1), Map.entry("uno", 1), Map.entry("una", 1),
            Map.entry("dos", 2), Map.entry("tres", 3), Map.entry("cuatro", 4),
            Map.entry("cinco", 5), Map.entry("seis", 6), Map.entry("siete", 7),
            Map.entry("ocho", 8), Map.entry("nueve", 9), Map.entry("diez", 10),
            Map.entry("once", 11), Map.entry("doce", 12), Map.entry("trece", 13),
            Map.entry("catorce", 14), Map.entry("quince", 15), Map.entry("veinte", 20));

    private static final int MAX_MEMORY_ENTRIES = 500;

    private final EventIndexService eventIndexService;
    private final EventSectionService sectionService;
    private final Map<String, String> lastFocusBySession;

    public ChatIntentResolver(EventIndexService eventIndexService, EventSectionService sectionService) {
        this.eventIndexService = eventIndexService;
        this.sectionService = sectionService;
        this.lastFocusBySession = Collections.synchronizedMap(
                new LinkedHashMap<>(64, 0.75f, false) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                        return size() > MAX_MEMORY_ENTRIES;
                    }
                });
    }

    public ResolvedIntent resolve(String sessionId, String message, List<EventDto> candidates) {
        String normalized = normalize(message);
        if (normalized.isBlank()) {
            return ResolvedIntent.none();
        }

        Intent intent = detectIntent(normalized);
        EventDto focus = matchEvent(normalized, candidates);
        if (focus == null) {
            focus = matchEvent(normalized, eventIndexService.all());
        }
        if (focus == null && intent != Intent.SEARCH && sessionId != null && !sessionId.isBlank()) {
            String rememberedId = lastFocusBySession.get(sessionId);
            if (rememberedId != null) {
                focus = eventIndexService.findById(rememberedId).orElse(null);
            }
        }
        if (focus == null) {
            return ResolvedIntent.none();
        }
        if (sessionId != null && !sessionId.isBlank()) {
            lastFocusBySession.put(sessionId, focus.id());
        }

        if (intent == Intent.SEARCH || intent == Intent.VIEW_EVENT) {
            return new ResolvedIntent(Intent.VIEW_EVENT, focus, List.of(), null, 0);
        }

        List<TicketSectionDto> sections = sectionService.sections(focus.id());
        if (sections.isEmpty()) {
            return new ResolvedIntent(Intent.VIEW_EVENT, focus, List.of(), null, 0);
        }

        TicketSectionDto section = matchSection(normalized, sections);
        int quantity = parseQuantity(normalized);

        if (intent == Intent.BUY && section != null) {
            return new ResolvedIntent(Intent.BUY, focus, sections, section, Math.max(1, quantity));
        }
        return new ResolvedIntent(Intent.VIEW_SECTIONS, focus, sections, section, Math.max(1, quantity));
    }

    private Intent detectIntent(String message) {
        if (containsPurchaseWord(message) || (parseQuantity(message) > 0 && containsTicketWord(message))) {
            return Intent.BUY;
        }
        if (containsAny(message, SECTION_WORDS, SECTION_PHRASES)) {
            return Intent.VIEW_SECTIONS;
        }
        if (containsAny(message, DETAIL_WORDS, DETAIL_PHRASES)) {
            return Intent.VIEW_EVENT;
        }
        return Intent.SEARCH;
    }

    private boolean containsPurchaseWord(String message) {
        for (String word : PURCHASE_WORDS) {
            if (containsWord(message, word)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsTicketWord(String message) {
        for (String word : TICKET_WORDS) {
            if (containsWord(message, word)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String message, Set<String> words, List<String> phrases) {
        for (String phrase : phrases) {
            if (message.contains(phrase)) {
                return true;
            }
        }
        for (String word : words) {
            if (containsWord(message, word)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsWord(String message, String word) {
        return Pattern.compile("\\b" + Pattern.quote(word) + "\\w*").matcher(message).find();
    }

    private EventDto matchEvent(String message, List<EventDto> events) {
        if (events == null || events.isEmpty()) {
            return null;
        }
        Set<String> messageTokens = tokens(message);
        if (messageTokens.isEmpty()) {
            return null;
        }
        EventDto best = null;
        int bestScore = 0;
        for (EventDto event : events) {
            if (event == null || event.name() == null) {
                continue;
            }
            int score = 0;
            for (String token : tokens(normalize(event.name()))) {
                if (messageTokens.contains(token)) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = event;
            }
        }
        return best;
    }

    private TicketSectionDto matchSection(String message, List<TicketSectionDto> sections) {
        if (sections == null || sections.isEmpty()) {
            return null;
        }
        if (sections.size() == 1) {
            return sections.get(0);
        }
        Set<String> messageTokens = tokens(message);
        TicketSectionDto best = null;
        int bestLength = -1;
        for (TicketSectionDto section : sections) {
            if (section.name() == null) {
                continue;
            }
            String name = normalize(section.name());
            boolean matched = (" " + message + " ").contains(" " + name + " ");
            if (!matched) {
                for (String token : tokens(name)) {
                    if (messageTokens.contains(token)) {
                        matched = true;
                        break;
                    }
                }
            }
            if (matched && name.length() > bestLength) {
                bestLength = name.length();
                best = section;
            }
        }
        return best;
    }

    private int parseQuantity(String message) {
        Matcher matcher = NUMBER.matcher(message);
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group());
            if (value > 0 && value <= 20) {
                return value;
            }
        }
        for (String token : message.split("\\s+")) {
            Integer value = WORD_NUMBERS.get(token);
            if (value != null) {
                return value;
            }
        }
        return 0;
    }

    private Set<String> tokens(String normalizedMessage) {
        String[] raw = NON_ALPHANUMERIC.split(normalizedMessage);
        Set<String> tokens = new HashSet<>();
        for (String token : raw) {
            if (token.length() >= 3 && !STOPWORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lowered = value.toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(lowered, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").trim();
    }
}
