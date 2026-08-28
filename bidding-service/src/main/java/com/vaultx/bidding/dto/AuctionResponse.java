package com.vaultx.bidding.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class AuctionResponse {
    private UUID id;
    private String title;
    private String description;
    private UUID sellerId;
    private BigDecimal startingPrice;
    private BigDecimal reservePrice;
    private BigDecimal currentBid;
    private BigDecimal bidIncrement;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime extendedAt;
    private int extensionPeriodSeconds;
    private String currency;
    private LocalDateTime createdAt;
    private String coverMediaUrl;
    private int bidCount;
}
