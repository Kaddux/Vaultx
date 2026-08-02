package com.vaultx.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionCreatedEvent {
    private UUID auctionId;
    private String title;
    private UUID sellerId;
    private BigDecimal startingPrice;
    private BigDecimal reservePrice;
    private BigDecimal bidIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String currency;
    private LocalDateTime createdAt;
}
