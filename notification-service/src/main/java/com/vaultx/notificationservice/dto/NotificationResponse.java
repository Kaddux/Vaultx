package com.vaultx.notificationservice.dto;

import com.vaultx.notificationservice.model.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String eventType,
        String channel,
        String title,
        String message,
        String status,
        LocalDateTime createdAt,
        LocalDateTime sentAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getEventType(),
                notification.getChannel(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getSentAt());
    }
}
