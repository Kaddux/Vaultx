package com.pm.transactionservice.repository;

import com.pm.transactionservice.model.Escrow;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EscrowRepository extends JpaRepository<Escrow, UUID> {

    Optional<Escrow> findByAuctionId(UUID auctionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Escrow e WHERE e.auctionId = :auctionId")
    Optional<Escrow> findByAuctionIdForUpdate(UUID auctionId);
}
