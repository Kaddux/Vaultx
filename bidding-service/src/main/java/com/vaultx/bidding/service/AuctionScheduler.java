package com.vaultx.bidding.service;

import com.vaultx.bidding.dto.event.AuctionEndedEvent;
import com.vaultx.bidding.metrics.BiddingMetrics;
import com.vaultx.bidding.model.Auction;
import com.vaultx.bidding.model.Bid;
import com.vaultx.bidding.model.OutboxEvent;
import com.vaultx.bidding.repository.AuctionRepository;
import com.vaultx.bidding.repository.BidRepository;
import com.vaultx.bidding.repository.OutboxEventRepository;
import com.vaultx.bidding.grpc.UserGrpcClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final BiddingMetrics biddingMetrics;
    private final UserGrpcClient userGrpcClient;

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void processAuctionStateTransitions() {
        LocalDateTime now = LocalDateTime.now();

        List<Auction> toStart = auctionRepository.findPendingToStart(now);
        for (Auction auction : toStart) {
            auction.setStatus("ACTIVE");
            auctionRepository.save(auction);
            log.info("Auction {} started", auction.getId());

            try {
                Map<String, Object> payload = Map.of(
                        "auctionId", auction.getId().toString(),
                        "sellerId", auction.getSellerId().toString(),
                        "startTime", auction.getStartTime().toString(),
                        "endTime", auction.getEndTime().toString());

                OutboxEvent outbox = new OutboxEvent();
                outbox.setAggregateType("AUCTION");
                outbox.setAggregateId(auction.getId().toString());
                outbox.setEventType("AUCTION_STARTED");
                outbox.setPayload(objectMapper.writeValueAsString(payload));
                outboxEventRepository.save(outbox);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize auction started event for {}", auction.getId(), e);
            }
        }

        List<Auction> toEnd = auctionRepository.findActiveToEnd(now);
        for (Auction auction : toEnd) {
            boolean reserveMet = auction.getReservePrice() == null
                    || (auction.getCurrentBid() != null
                        && auction.getCurrentBid().compareTo(auction.getReservePrice()) >= 0);

            // A winning auction waits for the buyer to pay (external payment) before it is SOLD.
            String status;
            if (auction.getCurrentBid() != null && reserveMet) {
                status = "AWAITING_PAYMENT";
            } else {
                status = "UNSOLD";
            }
            auction.setStatus(status);
            auctionRepository.save(auction);
            log.info("Auction {} ended with status {}", auction.getId(), status);
            biddingMetrics.recordAuctionEnded(status);

            UUID winnerId = null;
            if ("AWAITING_PAYMENT".equals(status)) {
                winnerId = bidRepository.findTopByAuctionIdOrderByAmountDesc(auction.getId())
                        .map(Bid::getBidderId).orElse(null);
            } else if ("UNSOLD".equals(status)) {
                // The auction didn't sell: release funds reserved by the top bid.
                bidRepository.findByAuctionIdAndStatusOrderByCreatedAtDesc(auction.getId(), "WINNING")
                        .forEach(bid -> releaseReserved(bid, auction.getId()));
            }

            try {
                AuctionEndedEvent payload = new AuctionEndedEvent(
                        auction.getId(), auction.getTitle(),
                        auction.getSellerId(), status,
                        auction.getCurrentBid(), winnerId, LocalDateTime.now());

                OutboxEvent ended = new OutboxEvent();
                ended.setAggregateType("AUCTION");
                ended.setAggregateId(auction.getId().toString());
                ended.setEventType("AUCTION_ENDED");
                ended.setPayload(objectMapper.writeValueAsString(payload));
                outboxEventRepository.save(ended);

                if ("AWAITING_PAYMENT".equals(status) && winnerId != null) {
                    OutboxEvent won = new OutboxEvent();
                    won.setAggregateType("AUCTION");
                    won.setAggregateId(winnerId.toString());
                    won.setEventType("AUCTION_WON");
                    won.setPayload(objectMapper.writeValueAsString(payload));
                    outboxEventRepository.save(won);
                }

                emitAuctionLostEvents(auction, payload, winnerId);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize auction ended event for {}", auction.getId(), e);
            }
        }
    }

    private void emitAuctionLostEvents(Auction auction, AuctionEndedEvent payload,
                                       UUID winnerId) throws JsonProcessingException {
        List<Bid> allBids = bidRepository.findByAuctionIdOrderByCreatedAtDesc(auction.getId());
        Set<UUID> notifiedBidders = new HashSet<>();
        for (Bid bid : allBids) {
            if (winnerId != null && bid.getBidderId().equals(winnerId)) {
                continue;
            }
            if (!notifiedBidders.add(bid.getBidderId())) {
                continue;
            }
            OutboxEvent lost = new OutboxEvent();
            lost.setAggregateType("AUCTION");
            lost.setAggregateId(bid.getBidderId().toString());
            lost.setEventType("AUCTION_LOST");
            lost.setPayload(objectMapper.writeValueAsString(payload));
            outboxEventRepository.save(lost);
        }
    }

    private void releaseReserved(Bid bid, UUID auctionId) {
        try {
            userGrpcClient.updateWallet(
                    bid.getBidderId().toString(),
                    bid.getAmount().doubleValue(),
                    "RELEASE",
                    "BIDREL_" + bid.getId().toString(),
                    "Release reserved funds for unsold auction " + auctionId);
        } catch (Exception e) {
            log.warn("Failed to release reserved funds for bid {} on auction {}: {}",
                    bid.getId(), auctionId, e.getMessage());
        }
    }
}
