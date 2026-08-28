package com.vaultx.notificationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vaultx.notificationservice.grpc.UserGrpcClient;
import com.vaultx.notificationservice.model.Notification;
import com.vaultx.notificationservice.model.UserPreference;
import com.vaultx.notificationservice.repository.NotificationRepository;
import com.vaultx.notificationservice.repository.UserPreferenceRepository;
import com.vaultx.notificationservice.service.NotificationTemplateBuilder.Template;
import com.vaultx.user.grpc.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final NotificationTemplateBuilder templateBuilder;
    private final ChannelService channelService;
    private final UserGrpcClient userGrpcClient;

    @Transactional
    public void processEvent(String topic, String key, JsonNode event) {
        Template template = templateBuilder.build(topic, key, event);
        if (template == null) {
            log.debug("No template for topic {}", topic);
            return;
        }

        UserProfile profile = userGrpcClient.getUserProfile(template.recipientId().toString());
        if (profile == null) {
            log.warn("User profile not found for {}", template.recipientId());
            return;
        }

        for (String channel : template.channels()) {
            if (!isEnabled(template.recipientId(), template.eventType(), channel)) {
                log.debug("User {} disabled {} for {}", template.recipientId(),
                        channel, template.eventType());
                continue;
            }

            // Dedupe on (recipient, eventType, kafkaKey, channel) so at-least-once
            // redelivery of the same event does not produce duplicate notifications.
            String dedupKey = dedupeKey(template.recipientId().toString(), template.eventType(), key, channel);
            if (notificationRepository
                    .existsByUserIdAndEventTypeAndDedupKey(template.recipientId(), template.eventType(), dedupKey)) {
                log.debug("Skipping duplicate notification {} {} '{}'", template.recipientId(), channel, key);
                continue;
            }

            boolean delivered = channelService.deliver(channel, profile, template);
            saveNotification(template, channel, delivered, dedupKey);
        }
    }

    private String dedupeKey(String recipientId, String eventType, String key, String channel) {
        return String.join("|", recipientId, eventType, key == null ? "" : key, channel);
    }

    private boolean isEnabled(UUID userId, String eventType, String channel) {
        Optional<UserPreference> preference =
                userPreferenceRepository.findByUserIdAndEventType(userId, eventType);
        if (preference.isEmpty()) return true;
        return switch (channel) {
            case "EMAIL" -> preference.get().isEmailEnabled();
            case "SMS" -> preference.get().isSmsEnabled();
            case "PUSH" -> preference.get().isPushEnabled();
            default -> true;
        };
    }

    private void saveNotification(Template template, String channel, boolean delivered, String dedupKey) {
        Notification notification = new Notification();
        notification.setUserId(template.recipientId());
        notification.setEventType(template.eventType());
        notification.setDedupKey(dedupKey);
        notification.setChannel(channel);
        notification.setTitle(template.title());
        notification.setMessage(template.message());
        notification.setStatus(delivered ? "SENT" : "FAILED");
        if (delivered) notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    public List<Notification> getNotifications(UUID userId, int page, int size) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .getContent();
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndStatusAndReadAtIsNull(userId, "SENT");
    }

    @Transactional
    public void markAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void updatePreference(UUID userId, String eventType,
                                 String channel, boolean enabled) {
        UserPreference preference = userPreferenceRepository
                .findByUserIdAndEventType(userId, eventType)
                .orElseGet(() -> {
                    UserPreference p = new UserPreference();
                    p.setUserId(userId);
                    p.setEventType(eventType);
                    return p;
                });

        switch (channel) {
            case "EMAIL" -> preference.setEmailEnabled(enabled);
            case "SMS" -> preference.setSmsEnabled(enabled);
            case "PUSH" -> preference.setPushEnabled(enabled);
            default -> throw new IllegalArgumentException("Unknown channel: " + channel);
        }
        userPreferenceRepository.save(preference);
    }
}
