package com.vaultx.bidding.repository;

import com.vaultx.bidding.model.Auction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuctionRepository extends JpaRepository<Auction, UUID> {

    List<Auction> findByStatus(String status);

    @Query("SELECT a FROM Auction a WHERE a.status = 'PENDING' AND a.startTime <= :now")
    List<Auction> findPendingToStart(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' AND a.endTime <= :now")
    List<Auction> findActiveToEnd(@Param("now") LocalDateTime now);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdWithLock(@Param("id") UUID id);

    List<Auction> findBySellerId(UUID id);

    List<Auction> findBySellerIdAndStatus(UUID sellerId, String status);
}
