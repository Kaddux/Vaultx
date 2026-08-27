package com.vaultx.bidding.dto;

import com.vaultx.bidding.model.AuctionMedia.AuctionMediaType;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignResponse {
    private UUID mediaId;
    private UUID auctionId;
    private AuctionMediaType mediaType;
    private String objectKey;
    private String contentType;
    private long sizeBytes;
    private String uploadUrl;
    private Map<String, String> headers;
    private long expiresInSeconds;
}
