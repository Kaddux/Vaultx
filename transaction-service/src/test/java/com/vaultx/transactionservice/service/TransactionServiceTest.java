package com.vaultx.transactionservice.service;

import com.vaultx.transactionservice.dto.TransactionResponse;
import com.vaultx.transactionservice.model.Transaction;
import com.vaultx.transactionservice.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void listForUser_returnsTransactionsNewestFirst() {
        UUID userId = UUID.randomUUID();
        Transaction txn = buildTransaction(userId, "ESCROW_HOLD", new BigDecimal("100.00"));
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(txn));

        List<TransactionResponse> responses = transactionService.listForUser(userId);

        assertEquals(1, responses.size());
        TransactionResponse r = responses.get(0);
        assertEquals(txn.getId(), r.id());
        assertEquals("ESCROW_HOLD", r.type());
        assertEquals(new BigDecimal("100.00"), r.amount());
        assertEquals("COMPLETED", r.status());
        verify(transactionRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void listForUser_noTransactions_returnsEmptyList() {
        UUID userId = UUID.randomUUID();
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of());

        assertEquals(0, transactionService.listForUser(userId).size());
    }

    private Transaction buildTransaction(UUID userId, String type, BigDecimal amount) {
        Transaction txn = new Transaction();
        txn.setId(UUID.randomUUID());
        txn.setUserId(userId);
        txn.setAuctionId(UUID.randomUUID());
        txn.setType(type);
        txn.setAmount(amount);
        txn.setCurrency("USD");
        txn.setStatus("COMPLETED");
        txn.setDescription("Test transaction");
        txn.setCreatedAt(LocalDateTime.now());
        txn.setCompletedAt(LocalDateTime.now());
        return txn;
    }
}
