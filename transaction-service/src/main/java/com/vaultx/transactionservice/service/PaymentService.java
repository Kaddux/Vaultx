package com.vaultx.transactionservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
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
import com.vaultx.user.grpc.WalletBalance;
import com.vaultx.user.grpc.WalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
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
    private final ObjectMapper objectMapper;
    private final PaymentMetrics paymentMetrics;
    private final UserGrpcClient userGrpcClient;

    @Value("${app.payment.currency}")
    private String currency;
    @Value("${app.payment.checkout-success-url}")
    private String successUrl;
    @Value("${app.payment.checkout-cancel-url}")
    private String cancelUrl;

    /**
     * Called on auction.won. Creates a Stripe Checkout Session (test mode) so the
     * winning buyer can pay online. The auction stays AWAITING_PAYMENT until the
     * checkout.session.completed webhook fires.
     */
    @Transactional
    public void processAuctionWin(AuctionEndedEvent event) {
        String idempotencyKey = "AUCTION_WON_" + event.getAuctionId();
        UUID auctionId = event.getAuctionId();
        UUID buyerId = event.getWinnerId();
        UUID sellerId = event.getSellerId();
        BigDecimal amount = event.getFinalBid();

        PaymentIntent existing = paymentIntentRepository
                .findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null && existing.getCheckoutUrl() != null
                && !"FAILED".equals(existing.getStatus())) {
            return; // already initiated
        }

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .putMetadata("auctionId", auctionId.toString())
                    .putMetadata("buyerId", buyerId.toString())
                    .putMetadata("sellerId", sellerId.toString())
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(currency)
                                    .setUnitAmount(amount.movePointRight(2).longValueExact())
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Auction " + auctionId)
                                            .build())
                                    .build())
                            .build())
                    .build();

            Session session = Session.create(params);

            PaymentIntent intent = new PaymentIntent();
            intent.setUserId(buyerId);
            intent.setSellerId(sellerId);
            intent.setAuctionId(auctionId);
            intent.setAmount(amount);
            intent.setStatus("AWAITING_PAYMENT");
            intent.setIdempotencyKey(idempotencyKey);
            intent.setCheckoutSessionId(session.getId());
            intent.setCheckoutUrl(session.getUrl());
            if (existing != null) {
                intent.setId(existing.getId());
            }
            paymentIntentRepository.save(intent);

            log.info("Created Stripe checkout session for auction {} ({}), amount={}",
                    auctionId, session.getId(), amount);

        } catch (StripeException e) {
            log.error("Failed to create Stripe Checkout Session for auction {}", auctionId, e);
            throw new RuntimeException("Failed to initiate payment", e);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentStatus(UUID auctionId) {
        PaymentIntent intent = paymentIntentRepository
                .findByIdempotencyKey("AUCTION_WON_" + auctionId).orElse(null);
        if (intent == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("auctionId", auctionId.toString());
        map.put("status", intent.getStatus());
        map.put("amount", intent.getAmount());
        map.put("buyerId", intent.getUserId() == null ? "" : intent.getUserId().toString());
        map.put("sellerId", intent.getSellerId() == null ? "" : intent.getSellerId().toString());
        map.put("walletDebited", intent.isWalletDebited());
        map.put("shortfall", intent.getShortfallNote());
        return map;
    }

    /** Returns the hosted checkout URL to redirect the winning buyer to, if initiated. */
    @Transactional(readOnly = true)
    public String getCheckoutUrl(UUID auctionId) {
        return paymentIntentRepository
                .findByIdempotencyKey("AUCTION_WON_" + auctionId)
                .map(PaymentIntent::getCheckoutUrl)
                .orElse(null);
    }

    /**
     * Verifies a Stripe Checkout session server-side and settles it.
     * Used as a local-dev fallback to the webhook: Stripe appends ?session_id= to
     * the success URL, and we confirm payment via the Stripe API (not a signature).
     */
    @Transactional
    public String confirmSession(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            if (!"paid".equals(session.getPaymentStatus())) {
                log.info("Stripe session {} not paid yet (status={})", sessionId, session.getPaymentStatus());
                return "NOT_PAID";
            }
            // Idempotent: handleCheckoutCompleted no-ops if already SUCCEEDED.
            handleCheckoutCompleted(sessionId);
            return "SUCCEEDED";
        } catch (StripeException e) {
            log.error("Stripe session confirm failed: sessionId={}", sessionId, e);
            throw new RuntimeException("Could not verify payment", e);
        }
    }

    @Transactional
    public void handleCheckoutCompleted(String sessionId) {
        PaymentIntent intent = paymentIntentRepository
                .findByCheckoutSessionId(sessionId).orElse(null);
        if (intent == null) { log.warn("Checkout completed for unknown session {}", sessionId); return; }
        if ("SUCCEEDED".equals(intent.getStatus())) return;

        UUID auctionId = intent.getAuctionId();
        UUID buyerId = intent.getUserId();
        UUID sellerId = intent.getSellerId();
        BigDecimal amount = intent.getAmount();

        // Affordability-gated wallet debit (demo wallet mirrors the purchase).
        // Prefer DEBIT so funds reserved at bid time are converted; fall back
        // to PURCHASE for listings that predate bid-time reservation.
        boolean debited = false;
        String shortfall = null;
        try {
            WalletBalance bal = userGrpcClient.getWalletBalance(buyerId.toString());
            if (BigDecimal.valueOf(bal.getBalance()).compareTo(amount) >= 0) {
                WalletResponse debit = userGrpcClient.updateWallet(
                        buyerId.toString(), -amount.doubleValue(), "DEBIT",
                        "SETTLE_" + intent.getIdempotencyKey(),
                        "Payment for auction " + auctionId);
                if ("FAILED".equals(debit.getStatus())
                        && debit.getFailureReason() != null
                        && debit.getFailureReason().toLowerCase().contains("reserved")) {
                    // No bid-time reservation on record; use the legacy purchase debit.
                    debit = userGrpcClient.updateWallet(
                            buyerId.toString(), -amount.doubleValue(), "PURCHASE",
                            "SETTLE_" + intent.getIdempotencyKey(),
                            "Payment for auction " + auctionId);
                }
                if ("SUCCESS".equals(debit.getStatus())) {
                    debited = true;
                } else {
                    shortfall = "Wallet debit failed: " + debit.getFailureReason();
                }
            } else {
                shortfall = "Insufficient wallet balance (" + bal.getBalance() + ")";
            }
        } catch (Exception e) {
            shortfall = "Wallet debit skipped: " + e.getMessage();
        }
        intent.setWalletDebited(debited);
        if (!debited) intent.setShortfallNote(shortfall);

        // (unchanged) escrow + ESCROW_HOLD txn
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
        txn.setIdempotencyKey("TXN_" + intent.getIdempotencyKey());
        txn.setCompletedAt(LocalDateTime.now());
        txn.setDescription("Escrow hold for auction: " + auctionId);
        transactionRepository.save(txn);

        intent.setStatus("SUCCEEDED");
        paymentIntentRepository.save(intent);

        saveOutboxEvent("PAYMENT_COMPLETED", auctionId.toString(), Map.of(
                "auctionId", auctionId.toString(),
                "buyerId", buyerId.toString(),
                "sellerId", sellerId.toString(),
                "amount", amount,
                "status", "COMPLETED"));
        paymentMetrics.recordPaymentCompleted();
        log.info("Payment completed via webhook: auctionId={}, debited={}, shortfall={}",
                auctionId, debited, shortfall);

    }

    /** Stripe webhook: checkout.session.expired -> mark FAILED, auction becomes UNSOLD. */
    @Transactional
    public void handleCheckoutFailed(String sessionId) {
        PaymentIntent intent = paymentIntentRepository
                .findByCheckoutSessionId(sessionId).orElse(null);
        if (intent == null) {
            log.warn("Checkout failed for unknown session {}", sessionId);
            return;
        }
        if ("SUCCEEDED".equals(intent.getStatus())) {
            return;
        }
        intent.setStatus("FAILED");
        intent.setFailureReason("Payment session expired or declined");
        paymentIntentRepository.save(intent);

        saveOutboxEvent("PAYMENT_FAILED", intent.getAuctionId().toString(), Map.of(
                "auctionId", intent.getAuctionId().toString(),
                "buyerId", intent.getUserId().toString(),
                "amount", intent.getAmount(),
                "reason", "Payment not completed"));
        paymentMetrics.recordPaymentFailed();
        log.warn("Payment failed via webhook: auctionId={}", intent.getAuctionId());
    }

    private void saveOutboxEvent(String eventType, String aggregateId, Map<String, Object> payload) {
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
