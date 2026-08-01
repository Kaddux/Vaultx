package com.pm.transactionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.transactionservice.model.OutboxEvent;
import com.pm.transactionservice.repository.OutboxEventRepository;
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
        if (unpublished.isEmpty()) return;

        log.debug("Transaction outbox: {} unpublished events", unpublished.size());

        for (OutboxEvent event : unpublished) {
            try {
                String topic = mapEventTypeToTopic(event.getEventType());
                Object payload = objectMapper.readValue(event.getPayload(), Map.class);

                kafkaTemplate.send(topic, event.getAggregateId(), payload)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                event.setPublished(true);
                                event.setPublishedAt(LocalDateTime.now());
                                outboxEventRepository.save(event);
                                log.debug("Published {} to {} (offset={})",
                                        event.getEventType(), topic,
                                        result.getRecordMetadata().offset());
                            } else {
                                log.error("Failed to publish {} (event id={}): {}",
                                        event.getEventType(), event.getId(), ex.getMessage());
                            }
                        });
            } catch (Exception e) {
                log.error("Error processing outbox event {}: {}",
                        event.getId(), e.getMessage());
            }
        }
    }

    private String mapEventTypeToTopic(String eventType) {
        return switch (eventType) {
            case "PAYMENT_COMPLETED" -> "payment.completed";
            case "PAYMENT_FAILED" -> "payment.failed";
            default -> throw new IllegalArgumentException(
                    "Unknown event type: " + eventType);
        };
    }
}
