package com.pm.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionEndedEvent {
    private UUID auctionId;
    private String title;
    private UUID sellerId;
    private String finalStatus;
    private BigDecimal finalBid;
    private UUID winnerId;
    private LocalDateTime endedAt;
}
