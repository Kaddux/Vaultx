package com.vaultx.bidding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultx.bidding.dto.WatchlistResponse;
import com.vaultx.bidding.security.JwtAuthenticationFilter;
import com.vaultx.bidding.security.JwtTokenProvider;
import com.vaultx.bidding.service.WatchlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WatchlistController.class)
@AutoConfigureMockMvc(addFilters = false)
class WatchlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WatchlistService watchlistService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final UUID testUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUserId, null, List.of()));
    }

    @Test
    void addToWatchlist_ShouldReturn201WhenAdded() throws Exception {
        UUID auctionId = UUID.randomUUID();
        when(watchlistService.add(any(UUID.class), any(UUID.class))).thenReturn(true);

        mockMvc.perform(post("/api/auctions/{auctionId}/watchlist", auctionId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WATCHING"));
    }

    @Test
    void addToWatchlist_AlreadyWatching_ShouldReturn200() throws Exception {
        UUID auctionId = UUID.randomUUID();
        when(watchlistService.add(any(UUID.class), any(UUID.class))).thenReturn(false);

        mockMvc.perform(post("/api/auctions/{auctionId}/watchlist", auctionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALREADY_WATCHING"));
    }

    @Test
    void removeFromWatchlist_ShouldReturn204() throws Exception {
        UUID auctionId = UUID.randomUUID();

        mockMvc.perform(delete("/api/auctions/{auctionId}/watchlist", auctionId))
                .andExpect(status().isNoContent());
    }

    @Test
    void getWatchlist_ShouldReturn200() throws Exception {
        WatchlistResponse response = new WatchlistResponse();
        response.setId(UUID.randomUUID());
        response.setTitle("Test Auction");
        response.setStatus("ACTIVE");
        response.setCurrentBid(new BigDecimal("150.00"));
        response.setEndTime(LocalDateTime.now().plusHours(1));

        when(watchlistService.listForUser(testUserId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/watchlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(response.getId().toString()))
                .andExpect(jsonPath("$[0].title").value("Test Auction"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }
}
