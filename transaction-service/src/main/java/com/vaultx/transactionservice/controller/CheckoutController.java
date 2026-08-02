package com.vaultx.transactionservice.controller;

import com.vaultx.transactionservice.dto.CheckoutRequest;
import com.vaultx.transactionservice.dto.RefundRequest;
import com.vaultx.transactionservice.model.Escrow;
import com.vaultx.transactionservice.repository.EscrowRepository;
import com.vaultx.transactionservice.service.EscrowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class CheckoutController {

    private final EscrowService escrowService;
    private final EscrowRepository escrowRepository;

    @PostMapping("/release")
    public ResponseEntity<Map<String, String>> releaseEscrow(
            @Valid @RequestBody CheckoutRequest request) {
        escrowService.releaseEscrow(request.getAuctionId());
        return ResponseEntity.ok(Map.of(
                "status", "RELEASED",
                "auctionId", request.getAuctionId().toString()
        ));
    }

    @PostMapping("/refund")
    public ResponseEntity<Map<String, String>> refundEscrow(
            @Valid @RequestBody RefundRequest request) {
        escrowService.refundEscrow(request.getAuctionId());
        return ResponseEntity.ok(Map.of(
                "status", "REFUNDED",
                "auctionId", request.getAuctionId().toString()
        ));
    }

    @GetMapping("/{auctionId}")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(
            @PathVariable UUID auctionId) {
        return escrowRepository.findByAuctionId(auctionId)
                .map(escrow -> ResponseEntity.ok(Map.<String, Object>of(
                        "auctionId", escrow.getAuctionId().toString(),
                        "status", escrow.getStatus(),
                        "amount", escrow.getAmount(),
                        "buyerId", escrow.getBuyerId().toString(),
                        "sellerId", escrow.getSellerId().toString(),
                        "createdAt", escrow.getCreatedAt().toString()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
