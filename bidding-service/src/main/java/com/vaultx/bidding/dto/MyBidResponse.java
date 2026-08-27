package com.vaultx.bidding.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class MyBidResponse {
    private UUID bidId;
    private UUID auctionId;
    private String auctionTitle;
    private String auctionStatus;
    private BigDecimal currentBid;
    private LocalDateTime endTime;
    private BigDecimal myBidAmount;
    private String myStatus;
    private LocalDateTime createdAt;
}
