package com.vaultx.userservice.controller;


import com.vaultx.userservice.DTO.WalletDepositRequest;
import com.vaultx.userservice.DTO.WalletResponse;
import com.vaultx.userservice.DTO.WalletTransactionResponse;
import com.vaultx.userservice.services.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(walletService.getByUserId(userId));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<WalletTransactionResponse>> getTransactions(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(walletService.getTransactions(userId));
    }

    @PostMapping("/deposit")
    public ResponseEntity<WalletResponse> deposit(@AuthenticationPrincipal UUID userId,
                                                  @Valid @RequestBody WalletDepositRequest request) {
        return ResponseEntity.ok(walletService.deposit(userId, request));
    }
}