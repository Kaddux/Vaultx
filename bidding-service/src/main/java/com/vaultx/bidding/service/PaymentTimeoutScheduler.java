package com.vaultx.bidding.service;

import com.vaultx.bidding.model.Auction;
import com.vaultx.bidding.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

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

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireUnpaidAuctions() {
        LocalDateTime threshold = LocalDateTime.now().minus(GRACE_PERIOD);
        List<Auction> unpaid = auctionRepository
                .findByStatusAndEndTimeBefore("AWAITING_PAYMENT", threshold);

        for (Auction auction : unpaid) {
            auction.setStatus("UNSOLD");
            auctionRepository.save(auction);
            log.warn("Auction {} expired unpaid -> UNSOLD", auction.getId());
        }
    }
}
