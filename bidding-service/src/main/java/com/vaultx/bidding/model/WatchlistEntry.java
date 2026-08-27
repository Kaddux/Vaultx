package com.vaultx.bidding.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "watchlist_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_watchlist_user_auction",
                columnNames = {"user_id", "auction_id"}))
@Getter
@Setter
public class WatchlistEntry {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "auction_id", nullable = false)
    private UUID auctionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
