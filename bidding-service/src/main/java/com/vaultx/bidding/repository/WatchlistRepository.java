package com.vaultx.bidding.repository;

import com.vaultx.bidding.model.WatchlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchlistRepository extends JpaRepository<WatchlistEntry, UUID> {

    List<WatchlistEntry> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<WatchlistEntry> findByUserIdAndAuctionId(UUID userId, UUID auctionId);

    boolean existsByUserIdAndAuctionId(UUID userId, UUID auctionId);

    void deleteByUserIdAndAuctionId(UUID userId, UUID auctionId);
}
