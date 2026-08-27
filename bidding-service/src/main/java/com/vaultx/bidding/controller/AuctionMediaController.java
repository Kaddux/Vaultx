package com.vaultx.bidding.controller;

import com.vaultx.bidding.dto.AuctionMediaResponse;
import com.vaultx.bidding.dto.MediaUploadRequest;
import com.vaultx.bidding.dto.PresignResponse;
import com.vaultx.bidding.service.AuctionMediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auctions/{auctionId}/media")
@RequiredArgsConstructor
public class AuctionMediaController {

    private final AuctionMediaService mediaService;

    @PostMapping
    public ResponseEntity<PresignResponse> createUpload(
            @AuthenticationPrincipal UUID sellerId,
            @PathVariable UUID auctionId,
            @Valid @RequestBody MediaUploadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediaService.create(auctionId, sellerId, request));
    }

    @GetMapping
    public ResponseEntity<List<AuctionMediaResponse>> list(@PathVariable UUID auctionId) {
        return ResponseEntity.ok(mediaService.list(auctionId));
    }

    @PostMapping("/{mediaId}/complete")
    public ResponseEntity<AuctionMediaResponse> complete(
            @AuthenticationPrincipal UUID sellerId,
            @PathVariable UUID auctionId,
            @PathVariable UUID mediaId) {
        return ResponseEntity.ok(mediaService.complete(auctionId, sellerId, mediaId));
    }

    @PutMapping("/{mediaId}/cover")
    public ResponseEntity<AuctionMediaResponse> setCover(
            @AuthenticationPrincipal UUID sellerId,
            @PathVariable UUID auctionId,
            @PathVariable UUID mediaId) {
        return ResponseEntity.ok(mediaService.setCover(auctionId, sellerId, mediaId));
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UUID sellerId,
            @PathVariable UUID auctionId,
            @PathVariable UUID mediaId) {
        mediaService.delete(auctionId, sellerId, mediaId);
        return ResponseEntity.noContent().build();
    }
}
