package com.vaultx.transactionservice.controller;

import com.vaultx.transactionservice.dto.TransactionResponse;
import com.vaultx.transactionservice.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Test
    void getMyTransactions_Returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        TransactionResponse response = new TransactionResponse(
                UUID.randomUUID(), userId, UUID.randomUUID(), "ESCROW_HOLD",
                new BigDecimal("100.00"), "USD", "COMPLETED",
                "Escrow hold for auction", LocalDateTime.now(), LocalDateTime.now());

        when(transactionService.listForUser(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/transactions")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("ESCROW_HOLD"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void getMyTransactions_NoTransactions_ReturnsEmptyArray() throws Exception {
        UUID userId = UUID.randomUUID();
        when(transactionService.listForUser(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
