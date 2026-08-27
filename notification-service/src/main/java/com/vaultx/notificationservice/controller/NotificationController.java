package com.vaultx.notificationservice.controller;

import com.vaultx.notificationservice.dto.NotificationResponse;
import com.vaultx.notificationservice.dto.PreferenceUpdateRequest;
import com.vaultx.notificationservice.model.Notification;
import com.vaultx.notificationservice.service.NotificationService;
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
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Notification> notifications =
                notificationService.getNotifications(currentUserId(userIdHeader), page, size);
        return ResponseEntity.ok(notifications.stream()
                .map(NotificationResponse::from)
                .toList());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        return ResponseEntity.ok(Map.of("unread",
                notificationService.getUnreadCount(currentUserId(userIdHeader))));
    }

    @PutMapping("/read")
    public ResponseEntity<Map<String, String>> markAllRead(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        notificationService.markAsRead(currentUserId(userIdHeader));
        return ResponseEntity.ok(Map.of("status", "READ"));
    }

    @PutMapping("/preferences/{eventType}")
    public ResponseEntity<Map<String, String>> updatePreference(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @PathVariable String eventType,
            @Valid @RequestBody PreferenceUpdateRequest request) {
        notificationService.updatePreference(currentUserId(userIdHeader), eventType,
                request.getChannel(), request.isEnabled());
        return ResponseEntity.ok(Map.of("status", "UPDATED"));
    }

    private UUID currentUserId(String userIdHeader) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                return UUID.fromString(userIdHeader);
            } catch (IllegalArgumentException ignored) {
                // fall through to placeholder
            }
        }
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
}
