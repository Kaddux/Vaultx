package com.vaultx.bidding.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class BidResponse {
    private UUID id;
    private UUID auctionId;
    private UUID bidderId;
    private BigDecimal amount;
    private BigDecimal maxAutoBid;
    private boolean isAutoBid;
    private String status;
    private BigDecimal currentHighestBid;
    private boolean isCurrentWinner;
    private LocalDateTime createdAt;
}
