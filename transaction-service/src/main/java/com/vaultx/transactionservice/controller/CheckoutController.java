package com.vaultx.transactionservice.controller;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.vaultx.transactionservice.dto.CheckoutRequest;
import com.vaultx.transactionservice.dto.RefundRequest;
import com.vaultx.transactionservice.model.Escrow;
import com.vaultx.transactionservice.repository.EscrowRepository;
import com.vaultx.transactionservice.service.EscrowService;
import com.vaultx.transactionservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class CheckoutController {

    private final EscrowService escrowService;
    private final EscrowRepository escrowRepository;
    private final PaymentService paymentService;

    @Value("${app.payment.stripe.webhook-secret}")
    private String webhookSecret;

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

    @GetMapping("/{auctionId}/session")
    public ResponseEntity<Map<String, Object>> getCheckoutSession(@PathVariable UUID auctionId) {
        String url = paymentService.getCheckoutUrl(auctionId);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<String> webhook(@RequestBody String payload,
                                          @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            switch (event.getType()) {
                case "checkout.session.completed" -> {
                    Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                    if (session != null) paymentService.handleCheckoutCompleted(session.getId());
                }
                case "checkout.session.expired" -> {
                    Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                    if (session != null) paymentService.handleCheckoutFailed(session.getId());
                }
                default -> log.debug("Ignoring Stripe event type {}", event.getType());
            }
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            log.error("Stripe webhook signature verification failed", e);
            return ResponseEntity.status(400).body("signature verification failed");
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirm(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "MISSING_SESSION_ID"));
        }
        String status = paymentService.confirmSession(sessionId);
        return ResponseEntity.ok(Map.of("status", status));
    }

    @GetMapping("/{auctionId}")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(
            @PathVariable UUID auctionId) {
        Map<String, Object> status = paymentService.getPaymentStatus(auctionId);
        return status == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(status);
    }
}
