package com.vaultx.userservice.DTO;

import com.vaultx.userservice.model.WalletTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletTransactionResponse(
        UUID id,
        String type,
        BigDecimal amount,
        String status,
        String description,
        LocalDateTime createdAt) {

    public static WalletTransactionResponse from(WalletTransaction tx) {
        return new WalletTransactionResponse(
                tx.getId(),
                tx.getTransactionType(),
                tx.getAmount(),
                tx.getStatus(),
                tx.getDescription(),
                tx.getCreatedAt());
    }
}
