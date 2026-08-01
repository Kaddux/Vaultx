package com.pm.transactionservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.transactionservice.grpc.UserGrpcClient;
import com.pm.transactionservice.model.Escrow;
import com.pm.transactionservice.model.OutboxEvent;
import com.pm.transactionservice.model.Transaction;
import com.pm.transactionservice.repository.EscrowRepository;
import com.pm.transactionservice.repository.OutboxEventRepository;
import com.pm.transactionservice.repository.TransactionRepository;
import com.vaultx.user.grpc.WalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscrowService {

    private final EscrowRepository escrowRepository;
    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final UserGrpcClient userGrpcClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public void releaseEscrow(UUID auctionId) {
        Escrow escrow = escrowRepository.findByAuctionIdForUpdate(auctionId)
                .orElseThrow(() -> new RuntimeException("Escrow not found: " + auctionId));

        if (!"HELD".equals(escrow.getStatus())) {
            throw new RuntimeException("Escrow not in HELD state: " + escrow.getStatus());
        }

        String idempotencyKey = "ESCROW_RELEASE_" + auctionId;
        WalletResponse response = userGrpcClient.updateWallet(
                escrow.getSellerId().toString(),
                escrow.getAmount().doubleValue(),
                "CREDIT",
                idempotencyKey,
                "Payment released for auction: " + auctionId
        );

        if (!"SUCCESS".equals(response.getStatus())) {
            throw new RuntimeException("Seller credit failed: " + response.getFailureReason());
        }

        escrow.setStatus("RELEASED");
        escrow.setReleasedAt(LocalDateTime.now());
        escrowRepository.save(escrow);

        Transaction txn = new Transaction();
        txn.setUserId(escrow.getSellerId());
        txn.setAuctionId(auctionId);
        txn.setType("ESCROW_RELEASE");
        txn.setAmount(escrow.getAmount());
        txn.setStatus("COMPLETED");
        txn.setIdempotencyKey(idempotencyKey);
        txn.setCompletedAt(LocalDateTime.now());
        txn.setDescription("Escrow released to seller for auction: " + auctionId);
        transactionRepository.save(txn);

        Map<String, Object> payload = Map.of(
                "auctionId", auctionId.toString(),
                "sellerId", escrow.getSellerId().toString(),
                "amount", escrow.getAmount(),
                "status", "RELEASED"
        );
        saveOutboxEvent("PAYMENT_COMPLETED", auctionId.toString(), payload);

        log.info("Escrow released: auctionId={}, sellerId={}, amount={}",
                auctionId, escrow.getSellerId(), escrow.getAmount());
    }

    @Transactional
    public void refundEscrow(UUID auctionId) {
        Escrow escrow = escrowRepository.findByAuctionIdForUpdate(auctionId)
                .orElseThrow(() -> new RuntimeException("Escrow not found: " + auctionId));

        if (!"HELD".equals(escrow.getStatus())) {
            throw new RuntimeException("Escrow not in HELD state: " + escrow.getStatus());
        }

        String idempotencyKey = "ESCROW_REFUND_" + auctionId;
        WalletResponse response = userGrpcClient.updateWallet(
                escrow.getBuyerId().toString(),
                escrow.getAmount().doubleValue(),
                "REFUND",
                idempotencyKey,
                "Refund for auction: " + auctionId
        );

        if (!"SUCCESS".equals(response.getStatus())) {
            throw new RuntimeException("Refund failed: " + response.getFailureReason());
        }

        escrow.setStatus("REFUNDED");
        escrow.setReleasedAt(LocalDateTime.now());
        escrowRepository.save(escrow);

        Transaction txn = new Transaction();
        txn.setUserId(escrow.getBuyerId());
        txn.setAuctionId(auctionId);
        txn.setType("ESCROW_REFUND");
        txn.setAmount(escrow.getAmount().negate());
        txn.setStatus("COMPLETED");
        txn.setIdempotencyKey(idempotencyKey);
        txn.setCompletedAt(LocalDateTime.now());
        txn.setDescription("Escrow refunded to buyer for auction: " + auctionId);
        transactionRepository.save(txn);

        log.info("Escrow refunded: auctionId={}, buyerId={}, amount={}",
                auctionId, escrow.getBuyerId(), escrow.getAmount());
    }

    private void saveOutboxEvent(String eventType, String aggregateId,
                                  Map<String, Object> payload) {
        try {
            OutboxEvent outbox = new OutboxEvent();
            outbox.setAggregateType("PAYMENT");
            outbox.setAggregateId(aggregateId);
            outbox.setEventType(eventType);
            outbox.setPayload(objectMapper.writeValueAsString(payload));
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox payload for eventType={}", eventType, e);
        }
    }
}
