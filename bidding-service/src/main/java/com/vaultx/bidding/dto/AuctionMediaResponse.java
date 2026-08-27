package com.vaultx.bidding.dto;

import com.vaultx.bidding.model.AuctionMedia.AuctionMediaType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionMediaResponse {
    private UUID id;
    private UUID auctionId;
    private AuctionMediaType mediaType;
    private String contentType;
    private long sizeBytes;
    private String url;
    private boolean cover;
    private int sortOrder;
    private String status;
    private LocalDateTime createdAt;
}
