package com.vaultx.bidding.service;

import com.vaultx.bidding.model.Auction;
import com.vaultx.bidding.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Soft-archives finished auctions (SOLD / UNSOLD) after their retention window.
 * Archived auctions are hidden from listings and direct IDs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionArchiveScheduler {

    private final AuctionRepository auctionRepository;

    @Value("${app.auction.archive-sold-after-days:90}")
    private long soldDays;

    @Value("${app.auction.archive-unsold-after-days:30}")
    private long unsoldDays;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void archiveFinishedAuctions() {
        List<Auction> sold = auctionRepository.findArchiveCandidates(
                List.of("SOLD"), LocalDateTime.now().minus(Duration.ofDays(soldDays)));
        List<Auction> unsold = auctionRepository.findArchiveCandidates(
                List.of("UNSOLD"), LocalDateTime.now().minus(Duration.ofDays(unsoldDays)));

        List<Auction> toArchive = new ArrayList<>(sold);
        toArchive.addAll(unsold);

        for (Auction auction : toArchive) {
            auction.setArchived(true);
            auction.setArchivedAt(LocalDateTime.now());
            auctionRepository.save(auction);
            log.info("Archived finished auction {} (status={})", auction.getId(), auction.getStatus());
        }
    }
}
