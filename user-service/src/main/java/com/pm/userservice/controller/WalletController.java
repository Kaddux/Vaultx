package com.pm.userservice.controller;


import com.pm.userservice.DTO.WalletDepositRequest;
import com.pm.userservice.DTO.WalletResponse;
import com.pm.userservice.services.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/deposit")
    public ResponseEntity<WalletResponse> deposit(@AuthenticationPrincipal UUID userId,
                                                  @Valid @RequestBody WalletDepositRequest request) {
        return ResponseEntity.ok(walletService.deposit(userId, request));
    }
}