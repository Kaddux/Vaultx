package com.vaultx.bidding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultx.bidding.dto.event.AuctionCreatedEvent;
import com.vaultx.bidding.dto.event.AuctionEndedEvent;
import com.vaultx.bidding.dto.event.BidPlacedEvent;
import com.vaultx.bidding.model.OutboxEvent;
import com.vaultx.bidding.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

        log.debug("Found {} unpublished outbox events", unpublished.size());

        for (OutboxEvent event : unpublished) {
            try {
                String topic = mapEventTypeToTopic(event.getEventType());

                kafkaTemplate.send(topic, event.getAggregateId(), deserializePayload(event))
                        .get(10, java.util.concurrent.TimeUnit.SECONDS);

                event.setPublished(true);
                event.setPublishedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
                log.debug("Published {} to {}", event.getEventType(), topic);
            } catch (Exception e) {
                log.error("Failed to publish {} (event id={}): {}", event.getEventType(),
                        event.getId(), e.getMessage());
            }
        }
    }

    private String mapEventTypeToTopic(String eventType) {
        return switch (eventType) {
            case "BID_PLACED" -> "bid.placed";
            case "AUCTION_CREATED" -> "auction.created";
            case "AUCTION_STARTED" -> "auction.started";
            case "AUCTION_ENDED" -> "auction.ended";
            case "AUCTION_WON" -> "auction.won";
            case "AUCTION_LOST" -> "auction.lost";
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }

    private Object deserializePayload(OutboxEvent event) throws Exception {
        return switch (event.getEventType()) {
            case "BID_PLACED" ->
                    objectMapper.readValue(event.getPayload(), BidPlacedEvent.class);
            case "AUCTION_CREATED" ->
                    objectMapper.readValue(event.getPayload(), AuctionCreatedEvent.class);
            case "AUCTION_ENDED", "AUCTION_WON", "AUCTION_LOST" ->
                    objectMapper.readValue(event.getPayload(), AuctionEndedEvent.class);
            case "AUCTION_STARTED" ->
                    objectMapper.readValue(event.getPayload(), java.util.Map.class);
            default ->
                    throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
        };
    }
}
