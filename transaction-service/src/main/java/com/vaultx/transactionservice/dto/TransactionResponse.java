package com.vaultx.transactionservice.dto;

import com.vaultx.transactionservice.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID userId,
        UUID auctionId,
        String type,
        BigDecimal amount,
        String currency,
        String status,
        String description,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {

    public static TransactionResponse from(Transaction txn) {
        return new TransactionResponse(
                txn.getId(),
                txn.getUserId(),
                txn.getAuctionId(),
                txn.getType(),
                txn.getAmount(),
                txn.getCurrency(),
                txn.getStatus(),
                txn.getDescription(),
                txn.getCreatedAt(),
                txn.getCompletedAt());
    }
}
