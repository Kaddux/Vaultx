package com.pm.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.userservice.DTO.WalletDepositRequest;
import com.pm.userservice.DTO.WalletResponse;
import com.pm.userservice.Exceptions.UserNotFoundException;
import com.pm.userservice.security.JwtAuthenticationFilter;
import com.pm.userservice.security.JwtTokenProvider;
import com.pm.userservice.services.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    private final UUID testUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUserId, null, List.of()));
    }

    @Test
    void getWallet_shouldReturnWalletResponse() throws Exception {
        UUID walletId = UUID.randomUUID();
        WalletResponse response = new WalletResponse();
        response.setId(walletId);
        response.setUserId(testUserId);
        response.setBalance(new BigDecimal("1000.00"));
        response.setReservedBalance(BigDecimal.ZERO);
        response.setAvailableBalance(new BigDecimal("1000.00"));
        response.setCurrency("USD");

        when(walletService.getByUserId(testUserId)).thenReturn(response);

        mockMvc.perform(get("/api/wallet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(walletId.toString()))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.balance").value(1000.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.availableBalance").value(1000.00));
    }

    @Test
    void getWallet_shouldReturnNotFoundWhenWalletNotFound() throws Exception {
        when(walletService.getByUserId(testUserId))
                .thenThrow(new UserNotFoundException("Wallet not found for user: " + testUserId));

        mockMvc.perform(get("/api/wallet"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deposit_shouldReturnUpdatedWalletResponse() throws Exception {
        WalletDepositRequest request = new WalletDepositRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setIdempotencyKey("key-123");

        UUID walletId = UUID.randomUUID();
        WalletResponse response = new WalletResponse();
        response.setId(walletId);
        response.setUserId(testUserId);
        response.setBalance(new BigDecimal("1500.00"));
        response.setReservedBalance(BigDecimal.ZERO);
        response.setAvailableBalance(new BigDecimal("1500.00"));
        response.setCurrency("USD");

        when(walletService.deposit(eq(testUserId), any(WalletDepositRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/wallet/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(1500.00))
                .andExpect(jsonPath("$.availableBalance").value(1500.00));
    }

    @Test
    void deposit_shouldReturnBadRequestWhenNegativeAmount() throws Exception {
        WalletDepositRequest request = new WalletDepositRequest();
        request.setAmount(new BigDecimal("-100.00"));
        request.setIdempotencyKey("key-456");

        mockMvc.perform(post("/api/wallet/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
