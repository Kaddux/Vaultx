package com.vaultx.bidding.service;

import com.vaultx.bidding.model.Auction;
import com.vaultx.bidding.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void processAuctionStateTransitions() {
        LocalDateTime now = LocalDateTime.now();

        List<Auction> toStart = auctionRepository.findPendingToStart(now);
        for (Auction auction : toStart) {
            auction.setStatus("ACTIVE");
            auctionRepository.save(auction);
            log.info("Auction {} started", auction.getId());
        }

        List<Auction> toEnd = auctionRepository.findActiveToEnd(now);
        for (Auction auction : toEnd) {
            boolean reserveMet = auction.getReservePrice() == null
                    || (auction.getCurrentBid() != null
                        && auction.getCurrentBid().compareTo(auction.getReservePrice()) >= 0);
            String status = (auction.getCurrentBid() != null && reserveMet) ? "SOLD" : "UNSOLD";
            auction.setStatus(status);
            auctionRepository.save(auction);
            log.info("Auction {} ended with status {}", auction.getId(), status);
        }
    }
}
