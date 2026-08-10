package com.vaultx.bidding.controller;

import com.vaultx.bidding.dto.AuctionRequest;
import com.vaultx.bidding.dto.AuctionResponse;
import com.vaultx.bidding.dto.BidRequest;
import com.vaultx.bidding.dto.BidResponse;
import com.vaultx.bidding.dto.MyBidResponse;
import com.vaultx.bidding.service.AuctionService;
import com.vaultx.bidding.service.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;
    private final BidService bidService;

    @PostMapping
    public ResponseEntity<AuctionResponse> create(
            @AuthenticationPrincipal UUID sellerId,
            @Valid @RequestBody AuctionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(auctionService.create(request, sellerId));
    }

    @GetMapping
    public ResponseEntity<List<AuctionResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID sellerId) {
        return ResponseEntity.ok(auctionService.getAll(status, sellerId));
    }

    @GetMapping("/bids/mine")
    public ResponseEntity<List<MyBidResponse>> getMyBidsAcrossAuctions(
            @AuthenticationPrincipal UUID bidderId) {
        return ResponseEntity.ok(bidService.getMyBidsAcrossAuctions(bidderId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuctionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(auctionService.getById(id));
    }

    @PostMapping("/{id}/bids")
    public ResponseEntity<BidResponse> placeBid(
            @AuthenticationPrincipal UUID bidderId,
            @PathVariable UUID id,
            @Valid @RequestBody BidRequest request) {
        return ResponseEntity.ok(bidService.placeBid(id, bidderId, request));
    }

    @GetMapping("/{id}/bids")
    public ResponseEntity<List<BidResponse>> getBids(@PathVariable UUID id) {
        return ResponseEntity.ok(bidService.getBidsForAuction(id));
    }

    @GetMapping("/{id}/bids/mine")
    public ResponseEntity<List<BidResponse>> getMyBids(
            @AuthenticationPrincipal UUID bidderId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(bidService.getMyBids(id, bidderId));
    }
}
