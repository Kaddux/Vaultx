package com.vaultx.bidding.service;

import com.vaultx.bidding.dto.WatchlistResponse;
import com.vaultx.bidding.model.Auction;
import com.vaultx.bidding.model.WatchlistEntry;
import com.vaultx.bidding.repository.AuctionRepository;
import com.vaultx.bidding.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final AuctionRepository auctionRepository;

    @Transactional
    public boolean add(UUID userId, UUID auctionId) {
        if (!auctionRepository.existsById(auctionId)) {
            throw new RuntimeException("Auction not found: " + auctionId);
        }
        if (watchlistRepository.existsByUserIdAndAuctionId(userId, auctionId)) {
            return false;
        }
        WatchlistEntry entry = new WatchlistEntry();
        entry.setUserId(userId);
        entry.setAuctionId(auctionId);
        watchlistRepository.save(entry);
        return true;
    }

    @Transactional
    public void remove(UUID userId, UUID auctionId) {
        watchlistRepository.deleteByUserIdAndAuctionId(userId, auctionId);
    }

    public List<WatchlistResponse> listForUser(UUID userId) {
        List<WatchlistEntry> entries = watchlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<UUID, Auction> auctions = loadAuctions(entries);
        return entries.stream()
                .map(entry -> toResponse(entry, auctions.get(entry.getAuctionId())))
                .toList();
    }

    private Map<UUID, Auction> loadAuctions(List<WatchlistEntry> entries) {
        List<UUID> auctionIds = entries.stream()
                .map(WatchlistEntry::getAuctionId)
                .distinct()
                .toList();
        return auctionIds.isEmpty()
                ? Map.of()
                : auctionRepository.findAllById(auctionIds).stream()
                        .collect(Collectors.toMap(Auction::getId, a -> a));
    }

    private WatchlistResponse toResponse(WatchlistEntry entry, Auction auction) {
        WatchlistResponse r = new WatchlistResponse();
        r.setId(entry.getAuctionId());
        r.setWatchedAt(entry.getCreatedAt());
        if (auction != null) {
            r.setTitle(auction.getTitle());
            r.setStatus(auction.getStatus());
            r.setCurrentBid(auction.getCurrentBid());
            r.setEndTime(auction.getEndTime());
            r.setSellerId(auction.getSellerId());
        }
        return r;
    }
}
