package com.vaultx.bidding.service;

import com.vaultx.bidding.dto.AuctionRequest;
import com.vaultx.bidding.dto.AuctionResponse;
import com.vaultx.bidding.dto.event.AuctionCreatedEvent;
import com.vaultx.bidding.metrics.BiddingMetrics;
import com.vaultx.bidding.model.Auction;
import com.vaultx.bidding.model.OutboxEvent;
import com.vaultx.bidding.repository.AuctionRepository;
import com.vaultx.bidding.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final BiddingMetrics biddingMetrics;

    @Transactional
    public AuctionResponse create(AuctionRequest request, UUID sellerId) {
        Auction auction = new Auction();
        auction.setId(UUID.randomUUID());
        auction.setTitle(request.getTitle());
        auction.setDescription(request.getDescription());
        auction.setSellerId(sellerId);
        auction.setStartingPrice(request.getStartingPrice());
        auction.setReservePrice(request.getReservePrice());
        auction.setBidIncrement(request.getBidIncrement());
        auction.setStartTime(request.getStartTime());
        auction.setEndTime(request.getEndTime());
        auction.setExtensionPeriodSeconds(request.getExtensionPeriodSeconds());
        auction.setCurrency(request.getCurrency());
        auction.setStatus("PENDING");
        Auction saved = auctionRepository.save(auction);

        try {
            AuctionCreatedEvent payload = new AuctionCreatedEvent(
                    saved.getId(), saved.getTitle(), sellerId,
                    saved.getStartingPrice(), saved.getReservePrice(),
                    saved.getBidIncrement(), saved.getStartTime(),
                    saved.getEndTime(), saved.getCurrency(), LocalDateTime.now());

            OutboxEvent outbox = new OutboxEvent();
            outbox.setAggregateType("AUCTION");
            outbox.setAggregateId(saved.getId().toString());
            outbox.setEventType("AUCTION_CREATED");
            outbox.setPayload(objectMapper.writeValueAsString(payload));
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize auction created event", e);
        }

        biddingMetrics.recordAuctionCreated();
        return toResponse(saved);
    }

    public AuctionResponse getById(UUID id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + id));
        return toResponse(auction);
    }

    public List<AuctionResponse> getAll(String statusFilter) {
        List<Auction> auctions = (statusFilter != null && !statusFilter.isBlank())
                ? auctionRepository.findByStatus(statusFilter.toUpperCase())
                : auctionRepository.findAll();
        return auctions.stream().map(this::toResponse).toList();
    }

    private AuctionResponse toResponse(Auction auction) {
        AuctionResponse r = new AuctionResponse();
        r.setId(auction.getId());
        r.setTitle(auction.getTitle());
        r.setDescription(auction.getDescription());
        r.setSellerId(auction.getSellerId());
        r.setStartingPrice(auction.getStartingPrice());
        r.setReservePrice(auction.getReservePrice());
        r.setCurrentBid(auction.getCurrentBid());
        r.setBidIncrement(auction.getBidIncrement());
        r.setStatus(auction.getStatus());
        r.setStartTime(auction.getStartTime());
        r.setEndTime(auction.getEndTime());
        r.setExtendedAt(auction.getExtendedAt());
        r.setExtensionPeriodSeconds(auction.getExtensionPeriodSeconds());
        r.setCurrency(auction.getCurrency());
        r.setCreatedAt(auction.getCreatedAt());
        return r;
    }
}
