package com.ticketstorm.catalog.api.rest;

import com.ticketstorm.catalog.application.service.EventCatalogService;
import com.ticketstorm.catalog.domain.model.Event;
import com.ticketstorm.catalog.domain.model.TicketSection;
import com.ticketstorm.catalog.domain.port.TicketSectionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class EventCatalogController {

    private final EventCatalogService catalogService;
    private final MeterRegistry meterRegistry;
    private final TicketSectionRepository ticketSectionRepository;

    public EventCatalogController(EventCatalogService catalogService, MeterRegistry meterRegistry, TicketSectionRepository ticketSectionRepository) {
        this.catalogService = catalogService;
        this.meterRegistry = meterRegistry;
        this.ticketSectionRepository = ticketSectionRepository;
    }

    @GetMapping
    public ResponseEntity<List<Event>> listEvents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo) {
        meterRegistry.counter("catalog.api.list").increment();
        List<Event> events = catalogService.listEvents(category, city, dateFrom, dateTo);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEvent(@PathVariable UUID id) {
        meterRegistry.counter("catalog.api.get").increment();
        return catalogService.getEvent(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Event>> searchEvents(@RequestParam(name = "q") String query) {
        meterRegistry.counter("catalog.api.search").increment();
        List<Event> results = catalogService.searchEvents(query);
        return ResponseEntity.ok(results);
    }

    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        meterRegistry.counter("catalog.api.create").increment();
        Event created = catalogService.createEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/events/" + created.getId()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable UUID id, @RequestBody Event event) {
        meterRegistry.counter("catalog.api.update").increment();
        Event updated = catalogService.updateEvent(id, event);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/sections")
    public ResponseEntity<List<TicketSection>> getEventSections(@PathVariable UUID id) {
        meterRegistry.counter("catalog.api.sections").increment();
        List<TicketSection> sections = ticketSectionRepository.findByEventIdOrderBySortOrderAsc(id);
        return ResponseEntity.ok(sections);
    }
}
