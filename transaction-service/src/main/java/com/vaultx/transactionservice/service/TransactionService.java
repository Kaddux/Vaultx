package com.vaultx.transactionservice.service;

import com.vaultx.transactionservice.dto.TransactionResponse;
import com.vaultx.transactionservice.grpc.UserGrpcClient;
import com.vaultx.transactionservice.repository.TransactionRepository;
import com.vaultx.user.grpc.WalletTransactionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserGrpcClient userGrpcClient;

    public List<TransactionResponse> listForUser(UUID userId) {
        List<TransactionResponse> merged = new ArrayList<>();

        // Escrow / payment transactions owned by this service.
        transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(TransactionResponse::from)
                .forEach(merged::add);

        // Wallet ledger (deposits, reserves, debits, credits) owned by user-service.
        try {
            userGrpcClient.getWalletTransactions(userId.toString())
                    .getTransactionsList()
                    .forEach(item -> merged.add(fromWalletItem(userId, item)));
        } catch (Exception e) {
            log.warn("Failed to load wallet ledger for user {}: {}", userId, e.getMessage());
        }

        merged.sort(Comparator.comparing(
                TransactionResponse::createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed());
        return merged;
    }

    private TransactionResponse fromWalletItem(UUID userId, WalletTransactionItem item) {
        LocalDateTime createdAt = LocalDateTime.MIN;
        try {
            createdAt = LocalDateTime.parse(item.getCreatedAt());
        } catch (Exception ignored) {
            // fall back to MIN on unparsable timestamps
        }
        return new TransactionResponse(
                UUID.fromString(item.getId()),
                userId,
                null,
                item.getTransactionType(),
                BigDecimal.valueOf(item.getAmount()),
                "USD",
                item.getStatus(),
                item.getDescription(),
                createdAt,
                createdAt);
    }
}
