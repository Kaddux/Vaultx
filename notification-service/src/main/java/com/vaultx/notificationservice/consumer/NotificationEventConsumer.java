package com.vaultx.notificationservice.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultx.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {
                    "user.registered", "auction.created", "auction.started",
                    "auction.ended", "bid.placed", "auction.won", "auction.lost",
                    "payment.completed", "payment.failed", "notification.requested"
            },
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(@Header(KafkaHeaders.RECEIVED_KEY) String key,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Payload String payload,
                        Acknowledgment acknowledgment) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            notificationService.processEvent(topic, key, node);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process notification event (topic={}, key={}, payload={}): {}",
                    topic, key, payload, e.getMessage(), e);
            throw new RuntimeException("Retry notification event processing", e);
        }
    }
}
