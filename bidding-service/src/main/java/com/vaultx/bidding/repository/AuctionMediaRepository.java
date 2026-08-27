package com.vaultx.bidding.repository;

import com.vaultx.bidding.model.AuctionMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuctionMediaRepository extends JpaRepository<AuctionMedia, UUID> {
    List<AuctionMedia> findByAuctionIdOrderBySortOrderAsc(UUID auctionId);
    long countByAuctionIdAndMediaTypeAndStatus(UUID auctionId, AuctionMedia.AuctionMediaType mediaType, String status);
    Optional<AuctionMedia> findFirstByAuctionIdAndCoverTrue(UUID auctionId);
}
