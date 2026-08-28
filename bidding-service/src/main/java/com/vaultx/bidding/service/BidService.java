package com.vaultx.bidding.service;

import com.vaultx.bidding.dto.BidRequest;
import com.vaultx.bidding.dto.BidResponse;
import com.vaultx.bidding.dto.MyBidResponse;
import com.vaultx.bidding.dto.event.BidPlacedEvent;
import com.vaultx.bidding.grpc.UserGrpcClient;
import com.vaultx.bidding.metrics.BiddingMetrics;
import com.vaultx.bidding.model.Auction;
import com.vaultx.bidding.model.Bid;
import com.vaultx.bidding.model.OutboxEvent;
import com.vaultx.bidding.repository.AuctionRepository;
import com.vaultx.bidding.repository.BidRepository;
import com.vaultx.bidding.repository.OutboxEventRepository;
import com.vaultx.user.grpc.UserProfile;
import com.vaultx.user.grpc.WalletBalance;
import com.vaultx.user.grpc.WalletResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BidService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final BiddingMetrics biddingMetrics;

    @Lazy
    @Autowired
    private UserGrpcClient userGrpcClient;

    @Transactional
    public BidResponse placeBid(UUID auctionId, UUID bidderId, BidRequest request) {
        long start = System.nanoTime();
        try {
            return doPlaceBid(auctionId, bidderId, request);
        } catch (RuntimeException e) {
            biddingMetrics.recordBidRejected();
            throw e;
        } finally {
            biddingMetrics.recordBidLatency(start);
        }
    }

    private BidResponse doPlaceBid(UUID auctionId, UUID bidderId, BidRequest request) {
        if (bidRepository.findByIdempotencyKey(request.getIdempotencyKey()).isPresent()) {
            throw new RuntimeException("Duplicate bid request");
        }
        WalletBalance wallet = userGrpcClient.getWalletBalance(bidderId.toString());
        BigDecimal availableBalance = BigDecimal.valueOf(wallet.getBalance())
                .subtract(BigDecimal.valueOf(wallet.getReservedBalance()));

        UserProfile profile = userGrpcClient.getUserProfile(bidderId.toString());
        if (profile == null || !"VERIFIED".equals(profile.getKycStatus())) {
            throw new RuntimeException("KYC verification required to place a bid");
        }

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

        // Capture the current winning bid so we can release its reservation on outbid.
        Bid previousWinner = null;
        List<Bid> winningBids = bidRepository
                .findByAuctionIdAndStatusOrderByCreatedAtDesc(auctionId, "WINNING");
        if (!winningBids.isEmpty()) {
            previousWinner = winningBids.get(0);
        }

        // Reserve the bid amount in the bidder's wallet (guarded, idempotent server-side).
        WalletResponse reserve = userGrpcClient.updateWallet(
                bidderId.toString(),
                request.getAmount().doubleValue(),
                "RESERVE",
                "BIDRES_" + request.getIdempotencyKey(),
                "Reserve for bid on auction " + auctionId);
        if (!"SUCCESS".equals(reserve.getStatus())) {
            throw new RuntimeException("Insufficient funds. " + reserve.getFailureReason());
        }

        try {
            bidRepository.markOutbidByAuction(auctionId);

            // Release the funds reserved by the bid we just outbid.
            if (previousWinner != null) {
                userGrpcClient.updateWallet(
                        previousWinner.getBidderId().toString(),
                        previousWinner.getAmount().doubleValue(),
                        "RELEASE",
                        "BIDREL_" + previousWinner.getId().toString(),
                        "Release reserved funds for outbid on auction " + auctionId);
            }
        } catch (RuntimeException e) {
            // Roll back our own reservation if the outbid/release path fails.
            userGrpcClient.updateWallet(
                    bidderId.toString(),
                    request.getAmount().doubleValue(),
                    "RELEASE",
                    "BIDREL_" + request.getIdempotencyKey().toString(),
                    "Release reserved funds (rollback) for auction " + auctionId);
            throw e;
        }

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

        biddingMetrics.recordBidPlaced();

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

    public List<MyBidResponse> getMyBidsAcrossAuctions(UUID bidderId) {
        List<Bid> bids = bidRepository.findByBidderIdOrderByCreatedAtDesc(bidderId);
        Map<UUID, Auction> auctions = loadAuctions(bids);
        return bids.stream().map(b -> toMyBidResponse(b, auctions.get(b.getAuctionId()))).toList();
    }

    private Map<UUID, Auction> loadAuctions(List<Bid> bids) {
        List<UUID> auctionIds = bids.stream()
                .map(Bid::getAuctionId)
                .distinct()
                .toList();
        return auctionIds.isEmpty()
                ? Map.of()
                : auctionRepository.findAllById(auctionIds).stream()
                        .collect(Collectors.toMap(Auction::getId, a -> a));
    }

    private MyBidResponse toMyBidResponse(Bid bid, Auction auction) {
        MyBidResponse r = new MyBidResponse();
        r.setBidId(bid.getId());
        r.setAuctionId(bid.getAuctionId());
        if (auction != null) {
            r.setAuctionTitle(auction.getTitle());
            r.setAuctionStatus(auction.getStatus());
            r.setCurrentBid(auction.getCurrentBid());
            r.setEndTime(auction.getEndTime());
        }
        r.setMyBidAmount(bid.getAmount());
        r.setMyStatus(bid.getStatus());
        r.setCreatedAt(bid.getCreatedAt());
        return r;
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
