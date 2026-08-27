package com.vaultx.bidding.controller;

import com.vaultx.bidding.dto.WatchlistResponse;
import com.vaultx.bidding.service.WatchlistService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @PostMapping("/api/auctions/{auctionId}/watchlist")
    public ResponseEntity<Map<String, String>> addToWatchlist(
            @AuthenticationPrincipal UUID userId,
            @PathVariable @NotNull UUID auctionId) {
        boolean added = watchlistService.add(userId, auctionId);
        return ResponseEntity.status(added ? HttpStatus.CREATED : HttpStatus.OK)
                .body(Map.of("status", added ? "WATCHING" : "ALREADY_WATCHING",
                        "auctionId", auctionId.toString()));
    }

    @DeleteMapping("/api/auctions/{auctionId}/watchlist")
    public ResponseEntity<Void> removeFromWatchlist(
            @AuthenticationPrincipal UUID userId,
            @PathVariable @NotNull UUID auctionId) {
        watchlistService.remove(userId, auctionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/watchlist")
    public ResponseEntity<List<WatchlistResponse>> getWatchlist(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(watchlistService.listForUser(userId));
    }
}
