package com.vaultx.bidding.service;

import com.vaultx.bidding.model.Auction;
import com.vaultx.bidding.model.Bid;
import com.vaultx.bidding.repository.AuctionRepository;
import com.vaultx.bidding.repository.BidRepository;
import com.vaultx.bidding.grpc.UserGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Flips AWAITING_PAYMENT auctions to UNSOLD when the winning buyer does not pay
 * within the configured grace period.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTimeoutScheduler {

    private static final Duration GRACE_PERIOD = Duration.ofHours(24);

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserGrpcClient userGrpcClient;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireUnpaidAuctions() {
        LocalDateTime threshold = LocalDateTime.now().minus(GRACE_PERIOD);
        List<Auction> unpaid = auctionRepository
                .findByStatusAndEndTimeBefore("AWAITING_PAYMENT", threshold);

        for (Auction auction : unpaid) {
            auction.setStatus("UNSOLD");
            auctionRepository.save(auction);
            // Release the winning bidder's funds since the auction never settled.
            bidRepository.findByAuctionIdAndStatusOrderByCreatedAtDesc(auction.getId(), "WINNING")
                    .forEach(bid -> releaseReserved(bid, auction.getId()));
            log.warn("Auction {} expired unpaid -> UNSOLD", auction.getId());
        }
    }

    private void releaseReserved(Bid bid, UUID auctionId) {
        try {
            userGrpcClient.updateWallet(
                    bid.getBidderId().toString(),
                    bid.getAmount().doubleValue(),
                    "RELEASE",
                    "BIDREL_" + bid.getId().toString(),
                    "Release reserved funds for unpaid auction " + auctionId);
        } catch (Exception e) {
            log.warn("Failed to release reserved funds for bid {} on auction {}: {}",
                    bid.getId(), auctionId, e.getMessage());
        }
    }
}
