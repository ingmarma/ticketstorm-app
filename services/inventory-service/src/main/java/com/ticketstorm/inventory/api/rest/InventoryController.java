package com.ticketstorm.inventory.api.rest;

import com.ticketstorm.inventory.application.service.InventoryService;
import com.ticketstorm.shared.common.dto.SeatDTO.SeatBlockRequest;
import com.ticketstorm.shared.common.dto.SeatDTO.SeatReserveRequest;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{eventId}/availability")
    @Timed(value = "inventory.get.availability", description = "Time to get availability")
    public ResponseEntity<Map<String, Long>> getAvailability(
            @PathVariable String eventId,
            @RequestParam(required = false) String sectionId) {

        log.debug("GET availability eventId={} sectionId={}", eventId, sectionId);

        if (sectionId != null) {
            return ResponseEntity.ok(inventoryService.getAvailableSeats(eventId, sectionId));
        }
        Map<String, Long> all = inventoryService.getAllAvailability(eventId)
                .getOrDefault("sections", Map.of());
        return ResponseEntity.ok(all);
    }

    @PostMapping("/{eventId}/block")
    @Timed(value = "inventory.block.seats", description = "Time to block seats")
    public ResponseEntity<Map<String, Object>> blockSeats(
            @PathVariable String eventId,
            @RequestBody SeatBlockRequest request) {

        log.info("POST block seats: eventId={} sectionId={} quantity={}",
                eventId, request.sectionId(), request.quantity());

        long remaining = inventoryService.blockSeats(eventId, request.sectionId(), request.quantity());
        return ResponseEntity.ok(Map.of(
                "eventId", eventId,
                "sectionId", request.sectionId(),
                "quantityBlocked", request.quantity(),
                "remainingStock", remaining
        ));
    }

    @PostMapping("/reserve")
    @Timed(value = "inventory.reserve.seat", description = "Time to reserve a seat")
    public ResponseEntity<Map<String, Object>> reserveSeat(
            @RequestBody SeatReserveRequest request) {

        log.info("POST reserve seat: eventId={} seatId={} userId={}",
                request.eventId(), request.seatId(), request.userId());

        var event = inventoryService.reserveSeat(
                request.eventId(), request.seatId(), request.userId(),
                request.idempotencyKey());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "seatId", event.getSeatId(),
                "eventId", event.getEventId(),
                "status", "RESERVED",
                "reservationId", event.getPayload() != null ? event.getPayload() : ""
        ));
    }
}
