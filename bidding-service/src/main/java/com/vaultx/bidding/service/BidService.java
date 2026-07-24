package com.vaultx.bidding.service;

import com.vaultx.bidding.dto.BidRequest;
import com.vaultx.bidding.dto.BidResponse;
import com.vaultx.bidding.dto.event.BidPlacedEvent;
import com.vaultx.bidding.grpc.UserGrpcClient;
import com.vaultx.bidding.model.Auction;
import com.vaultx.bidding.model.Bid;
import com.vaultx.bidding.model.OutboxEvent;
import com.vaultx.bidding.repository.AuctionRepository;
import com.vaultx.bidding.repository.BidRepository;
import com.vaultx.bidding.repository.OutboxEventRepository;
import com.vaultx.user.grpc.WalletBalance;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BidService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserGrpcClient userGrpcClient;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public BidResponse placeBid(UUID auctionId, UUID bidderId, BidRequest request) {
        if (bidRepository.findByIdempotencyKey(request.getIdempotencyKey()).isPresent()) {
            throw new RuntimeException("Duplicate bid request");
        }
        WalletBalance wallet = userGrpcClient.getWalletBalance(bidderId.toString());
        BigDecimal availableBalance = BigDecimal.valueOf(wallet.getBalance())
                .subtract(BigDecimal.valueOf(wallet.getReservedBalance()));

        if (request.getAmount().compareTo(availableBalance) > 0) {
            throw new RuntimeException("Insufficient funds. Available: " + availableBalance);
        }

        if (request.getMaxAutoBid() != null
                && request.getMaxAutoBid().compareTo(availableBalance) > 0) {
            throw new RuntimeException("Insufficient funds for max auto-bid");
        }

        Auction auction = auctionRepository.findByIdWithLock(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + auctionId));

        if (!"ACTIVE".equals(auction.getStatus())) {
            throw new RuntimeException("Auction is not active");
        }
        if (auction.getEndTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Auction has ended");
        }
        if (auction.getSellerId().equals(bidderId)) {
            throw new RuntimeException("Seller cannot bid on own auction");
        }

        BigDecimal minBid = auction.getCurrentBid() != null
                ? auction.getCurrentBid().add(auction.getBidIncrement())
                : auction.getStartingPrice();

        if (request.getAmount().compareTo(minBid) < 0) {
            throw new RuntimeException("Bid must be at least " + minBid);
        }

        bidRepository.markOutbidByAuction(auctionId);

        Bid bid = new Bid();
        bid.setId(UUID.randomUUID());
        bid.setAuctionId(auctionId);
        bid.setBidderId(bidderId);
        bid.setAmount(request.getAmount());
        bid.setMaxAutoBid(request.getMaxAutoBid());
        bid.setAutoBid(false);
        bid.setStatus("WINNING");
        bid.setIdempotencyKey(request.getIdempotencyKey());
        bidRepository.save(bid);

        auction.setCurrentBid(request.getAmount());
        auctionRepository.save(auction);

        try {
            BidPlacedEvent payload = new BidPlacedEvent(
                    bid.getId(), auctionId, bidderId, request.getAmount(),
                    auction.getCurrentBid(), LocalDateTime.now(),
                    request.getIdempotencyKey());

            OutboxEvent outbox = new OutboxEvent();
            outbox.setAggregateType("AUCTION");
            outbox.setAggregateId(auctionId.toString());
            outbox.setEventType("BID_PLACED");
            outbox.setPayload(objectMapper.writeValueAsString(payload));
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize bid placed event", e);
        }

        return toResponse(bid, auction);
    }

    public List<BidResponse> getBidsForAuction(UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        return bidRepository.findByAuctionIdOrderByCreatedAtDesc(auctionId)
                .stream().map(b -> toResponse(b, auction)).toList();
    }

    public List<BidResponse> getMyBids(UUID auctionId, UUID bidderId) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        return bidRepository.findByAuctionIdAndBidderIdOrderByCreatedAtDesc(auctionId, bidderId)
                .stream().map(b -> toResponse(b, auction)).toList();
    }

    private BidResponse toResponse(Bid bid, Auction auction) {
        BidResponse r = new BidResponse();
        r.setId(bid.getId());
        r.setAuctionId(bid.getAuctionId());
        r.setBidderId(bid.getBidderId());
        r.setAmount(bid.getAmount());
        r.setMaxAutoBid(bid.getMaxAutoBid());
        r.setAutoBid(bid.isAutoBid());
        r.setStatus(bid.getStatus());
        r.setCurrentHighestBid(auction != null ? auction.getCurrentBid() : null);
        r.setCurrentWinner("WINNING".equals(bid.getStatus()));
        r.setCreatedAt(bid.getCreatedAt());
        return r;
    }
}
