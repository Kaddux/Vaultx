package com.vaultx.transactionservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultx.transactionservice.dto.AuctionEndedEvent;
import com.vaultx.transactionservice.grpc.UserGrpcClient;
import com.vaultx.transactionservice.metrics.PaymentMetrics;
import com.vaultx.transactionservice.model.Escrow;
import com.vaultx.transactionservice.model.OutboxEvent;
import com.vaultx.transactionservice.model.PaymentIntent;
import com.vaultx.transactionservice.model.Transaction;
import com.vaultx.transactionservice.repository.EscrowRepository;
import com.vaultx.transactionservice.repository.OutboxEventRepository;
import com.vaultx.transactionservice.repository.PaymentIntentRepository;
import com.vaultx.transactionservice.repository.TransactionRepository;
import com.vaultx.user.grpc.WalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentIntentRepository paymentIntentRepository;
    private final EscrowRepository escrowRepository;
    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final UserGrpcClient userGrpcClient;
    private final ObjectMapper objectMapper;
    private final PaymentMetrics paymentMetrics;

    @Transactional
    public void processAuctionWin(AuctionEndedEvent event) {
        String idempotencyKey = "AUCTION_WON_" + event.getAuctionId();
        UUID auctionId = event.getAuctionId();
        UUID buyerId = event.getWinnerId();
        UUID sellerId = event.getSellerId();
        BigDecimal amount = event.getFinalBid();

        PaymentIntent existing = paymentIntentRepository
                .findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if ("SUCCEEDED".equals(existing.getStatus())) {
                log.info("Payment already processed: auctionId={}", auctionId);
                return;
            }
            if ("FAILED".equals(existing.getStatus())) {
                log.info("Retrying previously failed payment: auctionId={}", auctionId);
            }
        }

        PaymentIntent intent = new PaymentIntent();
        intent.setUserId(buyerId);
        intent.setAuctionId(auctionId);
        intent.setAmount(amount);
        intent.setStatus("PROCESSING");
        intent.setIdempotencyKey(idempotencyKey);
        if (existing != null) {
            intent.setId(existing.getId());
        }
        paymentIntentRepository.save(intent);

        try {
            WalletResponse debitResponse = userGrpcClient.updateWallet(
                    buyerId.toString(),
                    -amount.doubleValue(),
                    "DEBIT",
                    "DEBIT_" + idempotencyKey,
                    "Payment for auction: " + event.getTitle()
            );

            if (!"SUCCESS".equals(debitResponse.getStatus())) {
                throw new RuntimeException(
                        "Wallet debit failed: " + debitResponse.getFailureReason());
            }

            Escrow escrow = new Escrow();
            escrow.setAuctionId(auctionId);
            escrow.setBuyerId(buyerId);
            escrow.setSellerId(sellerId);
            escrow.setAmount(amount);
            escrow.setStatus("HELD");
            escrowRepository.save(escrow);

            Transaction txn = new Transaction();
            txn.setUserId(buyerId);
            txn.setAuctionId(auctionId);
            txn.setType("ESCROW_HOLD");
            txn.setAmount(amount);
            txn.setStatus("COMPLETED");
            txn.setIdempotencyKey("TXN_" + idempotencyKey);
            txn.setCompletedAt(LocalDateTime.now());
            txn.setDescription("Escrow hold for auction: " + event.getTitle());
            transactionRepository.save(txn);

            intent.setStatus("SUCCEEDED");
            paymentIntentRepository.save(intent);

            Map<String, Object> payload = Map.of(
                    "auctionId", auctionId.toString(),
                    "buyerId", buyerId.toString(),
                    "sellerId", sellerId.toString(),
                    "amount", amount,
                    "escrowId", escrow.getId().toString(),
                    "status", "COMPLETED",
                    "completedAt", LocalDateTime.now().toString()
            );
            saveOutboxEvent("PAYMENT_COMPLETED", auctionId.toString(), payload);

            paymentMetrics.recordPaymentCompleted();
            log.info("Payment completed: auctionId={}, amount={}", auctionId, amount);

        } catch (Exception e) {
            intent.setStatus("FAILED");
            intent.setFailureReason(e.getMessage());
            paymentIntentRepository.save(intent);

            Map<String, Object> failedPayload = Map.of(
                    "auctionId", auctionId.toString(),
                    "buyerId", buyerId.toString(),
                    "amount", amount,
                    "reason", e.getMessage()
            );
            saveOutboxEvent("PAYMENT_FAILED", auctionId.toString(), failedPayload);

            paymentMetrics.recordPaymentFailed();
            log.error("Payment failed: auctionId={}, error={}", auctionId, e.getMessage());
            throw new RuntimeException("Payment processing failed", e);
        }
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
