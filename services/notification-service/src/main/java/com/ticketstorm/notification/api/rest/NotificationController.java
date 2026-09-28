package com.ticketstorm.notification.api.rest;

import com.ticketstorm.notification.application.service.NotificationService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Timed(value = "notification.controller", description = "Notification controller metrics")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationService.NotificationRecord>> getRecentNotifications() {
        return ResponseEntity.ok(notificationService.getRecentNotifications());
    }
}
