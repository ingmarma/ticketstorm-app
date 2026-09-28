package com.ticketstorm.ai.application.service;

import com.ticketstorm.ai.application.dto.EventDto;
import com.ticketstorm.ai.application.dto.TicketSectionDto;
import com.ticketstorm.ai.application.service.ChatIntentResolver.Intent;
import com.ticketstorm.ai.application.service.ChatIntentResolver.ResolvedIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatIntentResolverTest {

    private static final String EVENT_ID = "a1000000-0000-0000-0000-000000000001";
    private static final String SESSION = "session-1";

    private final EventDto coldplay = new EventDto(
            EVENT_ID,
            "Coldplay - Music of the Spheres",
            "Concierto",
            "Estadio Defensores del Chaco",
            "Asunción",
            "CONCERT",
            Instant.parse("2026-12-10T00:00:00Z"),
            BigDecimal.valueOf(120000L),
            "PYG",
            null,
            20000,
            20000,
            null,
            null);

    private final List<TicketSectionDto> sections = List.of(
            new TicketSectionDto("s1", EVENT_ID, "VIP", null, BigDecimal.valueOf(1800000L), "PYG", 2000, 2000, 1),
            new TicketSectionDto("s2", EVENT_ID, "Platea A", null, BigDecimal.valueOf(450000L), "PYG", 3000, 3000, 2),
            new TicketSectionDto("s3", EVENT_ID, "Gradería B", null, BigDecimal.valueOf(120000L), "PYG", 1000, 1000, 3));

    private EventIndexService eventIndexService;
    private EventSectionService sectionService;
    private ChatIntentResolver resolver;

    @BeforeEach
    void setUp() {
        eventIndexService = mock(EventIndexService.class);
        sectionService = mock(EventSectionService.class);
        when(eventIndexService.all()).thenReturn(List.of(coldplay));
        when(eventIndexService.findById(anyString())).thenReturn(Optional.of(coldplay));
        when(sectionService.sections(EVENT_ID)).thenReturn(sections);
        resolver = new ChatIntentResolver(eventIndexService, sectionService);
    }

    @Test
    void sectionsQuestionReturnsEventSections() {
        ResolvedIntent resolved = resolver.resolve(SESSION, "¿Qué entradas hay para Coldplay?", List.of(coldplay));

        assertEquals(Intent.VIEW_SECTIONS, resolved.intent());
        assertEquals(EVENT_ID, resolved.focusEvent().id());
        assertEquals(3, resolved.sections().size());
        assertNull(resolved.section());
    }

    @Test
    void sectionPriceQuestionMatchesSection() {
        ResolvedIntent resolved = resolver.resolve(SESSION, "¿Cuánto sale Platea A para Coldplay?", List.of(coldplay));

        assertEquals(Intent.VIEW_SECTIONS, resolved.intent());
        assertNotNull(resolved.section());
        assertEquals("Platea A", resolved.section().name());
    }

    @Test
    void buyIntentWithSectionReturnsBuyAction() {
        ResolvedIntent resolved = resolver.resolve(SESSION, "Quiero comprar 2 entradas VIP para Coldplay", List.of(coldplay));

        assertEquals(Intent.BUY, resolved.intent());
        assertEquals(2, resolved.quantity());
        assertEquals("VIP", resolved.section().name());
        assertEquals(EVENT_ID, resolved.focusEvent().id());
    }

    @Test
    void buyIntentWithoutSectionFallsBackToSections() {
        ResolvedIntent resolved = resolver.resolve(SESSION, "Quiero comprar entradas para Coldplay", List.of(coldplay));

        assertEquals(Intent.VIEW_SECTIONS, resolved.intent());
        assertEquals(1, resolved.quantity());
        assertEquals(3, resolved.sections().size());
    }

    @Test
    void detailsQuestionReturnsViewEventWithoutSections() {
        ResolvedIntent resolved = resolver.resolve(SESSION, "Detalles de Coldplay", List.of(coldplay));

        assertEquals(Intent.VIEW_EVENT, resolved.intent());
        assertEquals(EVENT_ID, resolved.focusEvent().id());
        assertEquals(0, resolved.sections().size());
    }

    @Test
    void genericSearchKeepsNoAction() {
        ResolvedIntent resolved = resolver.resolve(SESSION, "¿Qué conciertos hay en noviembre?", List.of(coldplay));

        assertEquals(Intent.SEARCH, resolved.intent());
        assertNull(resolved.focusEvent());
        assertEquals(0, resolved.sections().size());
    }

    @Test
    void suggestionMessagesAreNotPurchaseIntent() {
        ResolvedIntent resolved = resolver.resolve(SESSION, "Quiero ir a ver fútbol", List.of(coldplay));

        assertEquals(Intent.SEARCH, resolved.intent());
        assertNull(resolved.focusEvent());
    }

    @Test
    void sectionsUseLastFocusedEventFromSession() {
        resolver.resolve(SESSION, "Cuéntame de Coldplay", List.of(coldplay));

        ResolvedIntent resolved = resolver.resolve(SESSION, "¿Cuánto sale VIP?", List.of());

        assertEquals(Intent.VIEW_SECTIONS, resolved.intent());
        assertEquals(EVENT_ID, resolved.focusEvent().id());
        assertNotNull(resolved.section());
        assertEquals("VIP", resolved.section().name());
    }

    @Test
    void accentInsensitiveSectionMatching() {
        ResolvedIntent resolved = resolver.resolve(SESSION, "cuanto cuesta graderia b de coldplay", List.of(coldplay));

        assertEquals(Intent.VIEW_SECTIONS, resolved.intent());
        assertNotNull(resolved.section());
        assertEquals("Gradería B", resolved.section().name());
    }
}
