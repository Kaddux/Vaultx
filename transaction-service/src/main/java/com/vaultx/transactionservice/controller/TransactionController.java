package com.vaultx.transactionservice.controller;

import com.vaultx.transactionservice.dto.TransactionResponse;
import com.vaultx.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getMyTransactions(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(transactionService.listForUser(userId));
    }
}
