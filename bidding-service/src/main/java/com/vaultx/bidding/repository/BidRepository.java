package com.vaultx.bidding.repository;

import com.vaultx.bidding.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BidRepository extends JpaRepository<Bid, UUID> {

    Optional<Bid> findTopByAuctionIdOrderByAmountDesc(UUID auctionId);

    Optional<Bid> findByIdempotencyKey(String idempotencyKey);

    List<Bid> findByAuctionIdOrderByCreatedAtDesc(UUID auctionId);

    List<Bid> findByAuctionIdAndBidderIdOrderByCreatedAtDesc(UUID auctionId, UUID bidderId);

    @Modifying
    @Query("UPDATE Bid b SET b.status = 'OUTBID' WHERE b.auctionId = :auctionId AND b.status = 'WINNING'")
    int markOutbidByAuction(@Param("auctionId") UUID auctionId);
}
