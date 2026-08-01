package com.pm.notificationservice.controller;

import com.pm.notificationservice.dto.NotificationResponse;
import com.pm.notificationservice.dto.PreferenceUpdateRequest;
import com.pm.notificationservice.model.Notification;
import com.pm.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Notification> notifications =
                notificationService.getNotifications(currentUserId(), page, size);
        return ResponseEntity.ok(notifications.stream()
                .map(NotificationResponse::from)
                .toList());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("unread", notificationService.getUnreadCount(currentUserId())));
    }

    @PutMapping("/preferences/{eventType}")
    public ResponseEntity<Map<String, String>> updatePreference(
            @PathVariable String eventType,
            @Valid @RequestBody PreferenceUpdateRequest request) {
        notificationService.updatePreference(currentUserId(), eventType,
                request.getChannel(), request.isEnabled());
        return ResponseEntity.ok(Map.of("status", "UPDATED"));
    }

    private UUID currentUserId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
}
