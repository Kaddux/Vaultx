package com.vaultx.userservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultx.userservice.model.OutboxEvent;
import com.vaultx.userservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEvent> unpublished = outboxEventRepository.findUnpublished();
        if (unpublished.isEmpty()) {
            return;
        }

        log.debug("User outbox: {} unpublished events", unpublished.size());

        for (OutboxEvent event : unpublished) {
            try {
                String topic = mapEventTypeToTopic(event.getEventType());
                Object payload = objectMapper.readValue(event.getPayload(), Map.class);

                kafkaTemplate.send(topic, event.getAggregateId(), payload)
                        .get(10, java.util.concurrent.TimeUnit.SECONDS);

                event.setPublished(true);
                event.setPublishedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
                log.debug("Published {} to {}", event.getEventType(), topic);
            } catch (Exception e) {
                log.error("Error processing outbox event {}: {}",
                        event.getId(), e.getMessage());
            }
        }
    }

    private String mapEventTypeToTopic(String eventType) {
        return switch (eventType) {
            case "USER_REGISTERED" -> "user.registered";
            case "NOTIFICATION_REQUESTED" -> "notification.requested";
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}
