package com.vaultx.bidding.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class WatchlistResponse {
    private UUID id;
    private String title;
    private String status;
    private BigDecimal currentBid;
    private LocalDateTime endTime;
    private UUID sellerId;
    private LocalDateTime watchedAt;
}
