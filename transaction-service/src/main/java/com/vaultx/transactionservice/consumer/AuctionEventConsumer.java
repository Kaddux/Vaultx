package com.vaultx.transactionservice.consumer;

import com.vaultx.transactionservice.dto.AuctionEndedEvent;
import com.vaultx.transactionservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "auction.won",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAuctionWon(AuctionEndedEvent event,
                                  Acknowledgment acknowledgment) {
        try {
            log.info("Consumed auction.won: auctionId={}, winnerId={}, amount={}",
                    event.getAuctionId(), event.getWinnerId(), event.getFinalBid());

            paymentService.processAuctionWin(event);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process auction.won: auctionId={}, error={}",
                    event.getAuctionId(), e.getMessage(), e);
            throw new RuntimeException("Retry auction.won processing", e);
        }
    }

    @KafkaListener(
            topics = "auction.lost",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAuctionLost(AuctionEndedEvent event,
                                   Acknowledgment acknowledgment) {
        try {
            log.info("Consumed auction.lost: auctionId={}", event.getAuctionId());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process auction.lost: {}", e.getMessage(), e);
            throw new RuntimeException("Retry auction.lost processing", e);
        }
    }
}
