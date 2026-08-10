package com.vaultx.bidding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultx.bidding.dto.AuctionRequest;
import com.vaultx.bidding.dto.AuctionResponse;
import com.vaultx.bidding.dto.BidRequest;
import com.vaultx.bidding.dto.BidResponse;
import com.vaultx.bidding.dto.MyBidResponse;
import com.vaultx.bidding.security.JwtAuthenticationFilter;
import com.vaultx.bidding.security.JwtTokenProvider;
import com.vaultx.bidding.service.AuctionService;
import com.vaultx.bidding.service.BidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuctionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuctionService auctionService;

    @MockBean
    private BidService bidService;

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
    void createAuction_ShouldReturn201() throws Exception {
        AuctionRequest request = buildAuctionRequest();
        AuctionResponse response = buildAuctionResponse();

        when(auctionService.create(any(AuctionRequest.class), any(UUID.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/auctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.title").value(response.getTitle()))
                .andExpect(jsonPath("$.status").value(response.getStatus()));
    }

    @Test
    void createAuction_BlankTitle_ShouldReturn400() throws Exception {
        AuctionRequest request = buildAuctionRequest();
        request.setTitle("");

        mockMvc.perform(post("/api/auctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllAuctions_ShouldReturn200() throws Exception {
        AuctionResponse response = buildAuctionResponse();

        when(auctionService.getAll(null, null))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/auctions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(response.getId().toString()))
                .andExpect(jsonPath("$[0].title").value(response.getTitle()));
    }

    @Test
    void getAllAuctions_WithStatusParam_ShouldCallGetAllWithStatus() throws Exception {
        AuctionResponse response = buildAuctionResponse();
        response.setStatus("ACTIVE");

        when(auctionService.getAll(eq("ACTIVE"), isNull()))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/auctions")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void getAllAuctions_WithSellerIdParam_ShouldCallGetAllWithSellerId() throws Exception {
        UUID sellerId = UUID.randomUUID();
        AuctionResponse response = buildAuctionResponse();

        when(auctionService.getAll(isNull(), eq(sellerId)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/auctions")
                        .param("sellerId", sellerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sellerId").value(response.getSellerId().toString()));
    }

    @Test
    void getMyBidsAcrossAuctions_ShouldReturn200() throws Exception {
        MyBidResponse response = buildMyBidResponse();

        when(bidService.getMyBidsAcrossAuctions(any(UUID.class)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/auctions/bids/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bidId").value(response.getBidId().toString()))
                .andExpect(jsonPath("$[0].auctionId").value(response.getAuctionId().toString()))
                .andExpect(jsonPath("$[0].myStatus").value(response.getMyStatus()));
    }

    @Test
    void getAuctionById_ShouldReturn200() throws Exception {
        UUID auctionId = UUID.randomUUID();
        AuctionResponse response = buildAuctionResponse();
        response.setId(auctionId);

        when(auctionService.getById(auctionId))
                .thenReturn(response);

        mockMvc.perform(get("/api/auctions/{id}", auctionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(auctionId.toString()))
                .andExpect(jsonPath("$.title").value(response.getTitle()));
    }

    @Test
    void getAuctionById_NotFound_ShouldReturn404() throws Exception {
        UUID auctionId = UUID.randomUUID();

        when(auctionService.getById(auctionId))
                .thenThrow(new RuntimeException("Auction not found: " + auctionId));

        mockMvc.perform(get("/api/auctions/{id}", auctionId))
                .andExpect(status().isNotFound());
    }

    @Test
    void placeBid_ShouldReturn200() throws Exception {
        UUID auctionId = UUID.randomUUID();
        BidRequest request = buildBidRequest();
        BidResponse response = buildBidResponse(auctionId);

        when(bidService.placeBid(eq(auctionId), any(UUID.class), any(BidRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/auctions/{id}/bids", auctionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.auctionId").value(auctionId.toString()));
    }

    @Test
    void placeBid_ValidationError_ShouldReturn400() throws Exception {
        UUID auctionId = UUID.randomUUID();
        BidRequest request = new BidRequest();
        request.setAmount(new BigDecimal("50.00"));

        mockMvc.perform(post("/api/auctions/{id}/bids", auctionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBidsForAuction_ShouldReturn200() throws Exception {
        UUID auctionId = UUID.randomUUID();
        BidResponse response = buildBidResponse(auctionId);

        when(bidService.getBidsForAuction(auctionId))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/auctions/{id}/bids", auctionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(response.getId().toString()))
                .andExpect(jsonPath("$[0].auctionId").value(auctionId.toString()));
    }

    @Test
    void getMyBids_ShouldReturn200() throws Exception {
        UUID auctionId = UUID.randomUUID();
        BidResponse response = buildBidResponse(auctionId);

        when(bidService.getMyBids(eq(auctionId), any(UUID.class)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/auctions/{id}/bids/mine", auctionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(response.getId().toString()))
                .andExpect(jsonPath("$[0].auctionId").value(auctionId.toString()));
    }

    private AuctionRequest buildAuctionRequest() {
        AuctionRequest request = new AuctionRequest();
        request.setTitle("Test Auction");
        request.setDescription("Description");
        request.setStartingPrice(new BigDecimal("100.00"));
        request.setReservePrice(new BigDecimal("200.00"));
        request.setBidIncrement(new BigDecimal("10.00"));
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusDays(1));
        request.setExtensionPeriodSeconds(120);
        request.setCurrency("USD");
        return request;
    }

    private AuctionResponse buildAuctionResponse() {
        AuctionResponse response = new AuctionResponse();
        response.setId(UUID.randomUUID());
        response.setTitle("Test Auction");
        response.setDescription("Description");
        response.setSellerId(UUID.randomUUID());
        response.setStartingPrice(new BigDecimal("100.00"));
        response.setReservePrice(new BigDecimal("200.00"));
        response.setCurrentBid(null);
        response.setBidIncrement(new BigDecimal("10.00"));
        response.setStatus("PENDING");
        response.setStartTime(LocalDateTime.now().plusHours(1));
        response.setEndTime(LocalDateTime.now().plusDays(1));
        response.setExtendedAt(null);
        response.setExtensionPeriodSeconds(120);
        response.setCurrency("USD");
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    private BidRequest buildBidRequest() {
        BidRequest request = new BidRequest();
        request.setAmount(new BigDecimal("150.00"));
        request.setMaxAutoBid(new BigDecimal("300.00"));
        request.setIdempotencyKey("idem-" + UUID.randomUUID());
        return request;
    }

    private MyBidResponse buildMyBidResponse() {
        MyBidResponse response = new MyBidResponse();
        response.setBidId(UUID.randomUUID());
        response.setAuctionId(UUID.randomUUID());
        response.setAuctionTitle("Test Auction");
        response.setAuctionStatus("ACTIVE");
        response.setCurrentBid(new BigDecimal("150.00"));
        response.setEndTime(LocalDateTime.now().plusHours(1));
        response.setMyBidAmount(new BigDecimal("150.00"));
        response.setMyStatus("WINNING");
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    private BidResponse buildBidResponse(UUID auctionId) {
        BidResponse response = new BidResponse();
        response.setId(UUID.randomUUID());
        response.setAuctionId(auctionId);
        response.setBidderId(UUID.randomUUID());
        response.setAmount(new BigDecimal("150.00"));
        response.setMaxAutoBid(new BigDecimal("300.00"));
        response.setAutoBid(false);
        response.setStatus("WINNING");
        response.setCurrentHighestBid(new BigDecimal("150.00"));
        response.setCurrentWinner(true);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }
}
