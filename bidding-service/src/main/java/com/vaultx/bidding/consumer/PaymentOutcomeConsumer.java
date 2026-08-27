package com.vaultx.bidding.consumer;

import com.vaultx.bidding.model.Auction;
import com.vaultx.bidding.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Applies the outcome of an external (Stripe) payment to the auction:
 * payment.completed -> SOLD, payment.failed -> UNSOLD.
 * Idempotent: transitions only from AWAITING_PAYMENT.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutcomeConsumer {

    private final AuctionRepository auctionRepository;

    @KafkaListener(topics = "payment.completed", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onPaymentCompleted(Map<String, Object> event) {
        applyOutcome(event, "SOLD");
    }

    @KafkaListener(topics = "payment.failed", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onPaymentFailed(Map<String, Object> event) {
        applyOutcome(event, "UNSOLD");
    }

    private void applyOutcome(Map<String, Object> event, String targetStatus) {
        Object auctionId = event.get("auctionId");
        if (auctionId == null) {
            log.warn("Payment outcome event missing auctionId: {}", event);
            return;
        }
        UUID id = UUID.fromString(auctionId.toString());
        Auction auction = auctionRepository.findById(id).orElse(null);
        if (auction == null) {
            log.warn("Auction not found for payment outcome: {}", id);
            return;
        }
        if (!"AWAITING_PAYMENT".equals(auction.getStatus())) {
            return; // already settled/timeout
        }
        auction.setStatus(targetStatus);
        auctionRepository.save(auction);
        log.info("Auction {} -> {} via payment outcome", id, targetStatus);
    }
}
