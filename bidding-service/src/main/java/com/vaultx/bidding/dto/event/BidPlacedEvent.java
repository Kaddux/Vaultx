package com.vaultx.bidding.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidPlacedEvent {
    private UUID bidId;
    private UUID auctionId;
    private UUID bidderId;
    private BigDecimal amount;
    private BigDecimal currentHighestBid;
    private LocalDateTime timestamp;
    private String idempotencyKey;
}
