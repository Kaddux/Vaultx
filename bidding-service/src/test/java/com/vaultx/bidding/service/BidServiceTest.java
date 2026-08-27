package com.vaultx.bidding.service;

import com.vaultx.bidding.dto.BidRequest;
import com.vaultx.bidding.dto.BidResponse;
import com.vaultx.bidding.grpc.UserGrpcClient;
import com.vaultx.bidding.metrics.BiddingMetrics;
import com.vaultx.bidding.model.Auction;
import com.vaultx.bidding.model.Bid;
import com.vaultx.bidding.model.OutboxEvent;
import com.vaultx.bidding.repository.AuctionRepository;
import com.vaultx.bidding.repository.BidRepository;
import com.vaultx.bidding.repository.OutboxEventRepository;
import com.vaultx.user.grpc.UserProfile;
import com.vaultx.user.grpc.WalletBalance;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private UserGrpcClient userGrpcClient;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private BiddingMetrics biddingMetrics;

    @InjectMocks
    private BidService bidService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bidService, "userGrpcClient", userGrpcClient);
    }

    @Test
    void placeBid_success_validBid_returnsBidResponseWithWinningStatus() throws Exception {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        BidRequest request = buildBidRequest(new BigDecimal("150.00"), "idempotent-key-1");

        WalletBalance walletBalance = mock(WalletBalance.class);
        when(walletBalance.getBalance()).thenReturn(10000.0);
        when(walletBalance.getReservedBalance()).thenReturn(0.0);
        when(userGrpcClient.getWalletBalance(anyString())).thenReturn(walletBalance);
        when(userGrpcClient.getUserProfile(anyString())).thenReturn(buildVerifiedProfile());

        when(bidRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());

        Auction auction = buildAuction(auctionId, sellerId);
        auction.setStatus("ACTIVE");
        auction.setCurrentBid(new BigDecimal("100.00"));
        auction.setBidIncrement(new BigDecimal("10.00"));
        auction.setStartTime(LocalDateTime.now().minusHours(1));
        auction.setEndTime(LocalDateTime.now().plusDays(1));
        when(auctionRepository.findByIdWithLock(auctionId)).thenReturn(Optional.of(auction));

        when(bidRepository.markOutbidByAuction(auctionId)).thenReturn(1);
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        BidResponse response = bidService.placeBid(auctionId, bidderId, request);

        assertNotNull(response);
        assertEquals("WINNING", response.getStatus());
        assertEquals(request.getAmount(), response.getAmount());
        assertEquals(auctionId, response.getAuctionId());
        assertEquals(bidderId, response.getBidderId());
        assertTrue(response.isCurrentWinner());

        verify(bidRepository).markOutbidByAuction(auctionId);
        verify(bidRepository).save(any(Bid.class));
        verify(auctionRepository).save(any(Auction.class));
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void placeBid_error_duplicateIdempotencyKey_throwsRuntimeException() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        BidRequest request = buildBidRequest(new BigDecimal("150.00"), "duplicate-key");

        Bid existingBid = new Bid();
        existingBid.setId(UUID.randomUUID());
        existingBid.setIdempotencyKey("duplicate-key");
        when(bidRepository.findByIdempotencyKey("duplicate-key")).thenReturn(Optional.of(existingBid));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bidService.placeBid(auctionId, bidderId, request));

        assertEquals("Duplicate bid request", exception.getMessage());
        verify(bidRepository).findByIdempotencyKey("duplicate-key");
        verify(auctionRepository, never()).findByIdWithLock(any());
    }

    @Test
    void placeBid_error_insufficientFunds_throwsRuntimeException() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        BidRequest request = buildBidRequest(new BigDecimal("500.00"), "key-insufficient");

        when(bidRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());

        WalletBalance walletBalance = mock(WalletBalance.class);
        when(walletBalance.getBalance()).thenReturn(300.0);
        when(walletBalance.getReservedBalance()).thenReturn(0.0);
        when(userGrpcClient.getWalletBalance(anyString())).thenReturn(walletBalance);
        when(userGrpcClient.getUserProfile(anyString())).thenReturn(buildVerifiedProfile());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bidService.placeBid(auctionId, bidderId, request));

        assertTrue(exception.getMessage().contains("Insufficient funds"));
        verify(auctionRepository, never()).findByIdWithLock(any());
    }

    @Test
    void placeBid_error_auctionNotFound_throwsRuntimeException() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        BidRequest request = buildBidRequest(new BigDecimal("150.00"), "key-notfound");

        when(bidRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());

        WalletBalance walletBalance = mock(WalletBalance.class);
        when(walletBalance.getBalance()).thenReturn(10000.0);
        when(walletBalance.getReservedBalance()).thenReturn(0.0);
        when(userGrpcClient.getWalletBalance(anyString())).thenReturn(walletBalance);
        when(userGrpcClient.getUserProfile(anyString())).thenReturn(buildVerifiedProfile());

        when(auctionRepository.findByIdWithLock(auctionId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bidService.placeBid(auctionId, bidderId, request));

        assertTrue(exception.getMessage().contains("Auction not found"));
        assertTrue(exception.getMessage().contains(auctionId.toString()));
    }

    @Test
    void placeBid_error_auctionNotActive_throwsRuntimeException() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        BidRequest request = buildBidRequest(new BigDecimal("150.00"), "key-notactive");

        when(bidRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());

        WalletBalance walletBalance = mock(WalletBalance.class);
        when(walletBalance.getBalance()).thenReturn(10000.0);
        when(walletBalance.getReservedBalance()).thenReturn(0.0);
        when(userGrpcClient.getWalletBalance(anyString())).thenReturn(walletBalance);
        when(userGrpcClient.getUserProfile(anyString())).thenReturn(buildVerifiedProfile());

        Auction auction = buildAuction(auctionId, sellerId);
        auction.setStatus("PENDING");
        auction.setEndTime(LocalDateTime.now().plusDays(1));
        when(auctionRepository.findByIdWithLock(auctionId)).thenReturn(Optional.of(auction));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bidService.placeBid(auctionId, bidderId, request));

        assertEquals("Auction is not active", exception.getMessage());
    }

    @Test
    void placeBid_error_auctionEnded_throwsRuntimeException() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        BidRequest request = buildBidRequest(new BigDecimal("150.00"), "key-ended");

        when(bidRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());

        WalletBalance walletBalance = mock(WalletBalance.class);
        when(walletBalance.getBalance()).thenReturn(10000.0);
        when(walletBalance.getReservedBalance()).thenReturn(0.0);
        when(userGrpcClient.getWalletBalance(anyString())).thenReturn(walletBalance);
        when(userGrpcClient.getUserProfile(anyString())).thenReturn(buildVerifiedProfile());

        Auction auction = buildAuction(auctionId, sellerId);
        auction.setStatus("ACTIVE");
        auction.setEndTime(LocalDateTime.now().minusHours(1));
        when(auctionRepository.findByIdWithLock(auctionId)).thenReturn(Optional.of(auction));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bidService.placeBid(auctionId, bidderId, request));

        assertEquals("Auction has ended", exception.getMessage());
    }

    @Test
    void placeBid_error_sellerBidsOnOwnAuction_throwsRuntimeException() {
        UUID auctionId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        BidRequest request = buildBidRequest(new BigDecimal("150.00"), "key-seller");

        when(bidRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());

        WalletBalance walletBalance = mock(WalletBalance.class);
        when(walletBalance.getBalance()).thenReturn(10000.0);
        when(walletBalance.getReservedBalance()).thenReturn(0.0);
        when(userGrpcClient.getWalletBalance(anyString())).thenReturn(walletBalance);
        when(userGrpcClient.getUserProfile(anyString())).thenReturn(buildVerifiedProfile());

        Auction auction = buildAuction(auctionId, sellerId);
        auction.setStatus("ACTIVE");
        auction.setEndTime(LocalDateTime.now().plusDays(1));
        when(auctionRepository.findByIdWithLock(auctionId)).thenReturn(Optional.of(auction));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bidService.placeBid(auctionId, sellerId, request));

        assertEquals("Seller cannot bid on own auction", exception.getMessage());
    }

    @Test
    void placeBid_error_bidTooLow_throwsRuntimeException() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        BidRequest request = buildBidRequest(new BigDecimal("95.00"), "key-toolow");

        when(bidRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());

        WalletBalance walletBalance = mock(WalletBalance.class);
        when(walletBalance.getBalance()).thenReturn(10000.0);
        when(walletBalance.getReservedBalance()).thenReturn(0.0);
        when(userGrpcClient.getWalletBalance(anyString())).thenReturn(walletBalance);
        when(userGrpcClient.getUserProfile(anyString())).thenReturn(buildVerifiedProfile());

        Auction auction = buildAuction(auctionId, sellerId);
        auction.setStatus("ACTIVE");
        auction.setCurrentBid(new BigDecimal("100.00"));
        auction.setBidIncrement(new BigDecimal("10.00"));
        auction.setEndTime(LocalDateTime.now().plusDays(1));
        when(auctionRepository.findByIdWithLock(auctionId)).thenReturn(Optional.of(auction));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bidService.placeBid(auctionId, bidderId, request));

        assertTrue(exception.getMessage().contains("Bid must be at least"));
    }

    @Test
    void getBidsForAuction_success_returnsListOfBids() {
        UUID auctionId = UUID.randomUUID();
        Auction auction = buildAuction(auctionId, UUID.randomUUID());
        Bid bid = buildBid(auctionId, UUID.randomUUID(), new BigDecimal("150.00"));

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(bidRepository.findByAuctionIdOrderByCreatedAtDesc(auctionId)).thenReturn(List.of(bid));

        List<BidResponse> responses = bidService.getBidsForAuction(auctionId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(bid.getId(), responses.get(0).getId());
        assertEquals(bid.getAuctionId(), responses.get(0).getAuctionId());
        assertEquals(bid.getBidderId(), responses.get(0).getBidderId());
        assertEquals(bid.getAmount(), responses.get(0).getAmount());
        assertEquals(bid.getStatus(), responses.get(0).getStatus());

        verify(auctionRepository).findById(auctionId);
        verify(bidRepository).findByAuctionIdOrderByCreatedAtDesc(auctionId);
    }

    @Test
    void getBidsForAuction_success_emptyListWhenNoBids() {
        UUID auctionId = UUID.randomUUID();

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.empty());
        when(bidRepository.findByAuctionIdOrderByCreatedAtDesc(auctionId)).thenReturn(Collections.emptyList());

        List<BidResponse> responses = bidService.getBidsForAuction(auctionId);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void getMyBids_success_returnsListFilteredByBidder() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        Auction auction = buildAuction(auctionId, UUID.randomUUID());
        Bid bid = buildBid(auctionId, bidderId, new BigDecimal("200.00"));

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(bidRepository.findByAuctionIdAndBidderIdOrderByCreatedAtDesc(auctionId, bidderId))
                .thenReturn(List.of(bid));

        List<BidResponse> responses = bidService.getMyBids(auctionId, bidderId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(bid.getId(), responses.get(0).getId());
        assertEquals(bidderId, responses.get(0).getBidderId());

        verify(auctionRepository).findById(auctionId);
        verify(bidRepository).findByAuctionIdAndBidderIdOrderByCreatedAtDesc(auctionId, bidderId);
    }

    @Test
    void getMyBids_success_returnsEmptyListWhenNoBidsFromBidder() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.empty());
        when(bidRepository.findByAuctionIdAndBidderIdOrderByCreatedAtDesc(auctionId, bidderId))
                .thenReturn(Collections.emptyList());

        List<BidResponse> responses = bidService.getMyBids(auctionId, bidderId);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void placeBid_error_notKycVerified_throwsRuntimeException() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        BidRequest request = buildBidRequest(new BigDecimal("150.00"), "key-nokyc");

        when(bidRepository.findByIdempotencyKey(request.getIdempotencyKey())).thenReturn(Optional.empty());

        WalletBalance walletBalance = mock(WalletBalance.class);
        when(walletBalance.getBalance()).thenReturn(10000.0);
        when(walletBalance.getReservedBalance()).thenReturn(0.0);
        when(userGrpcClient.getWalletBalance(anyString())).thenReturn(walletBalance);

        UserProfile unverified = UserProfile.newBuilder()
                .setUserId(bidderId.toString())
                .setKycStatus("PENDING")
                .build();
        when(userGrpcClient.getUserProfile(anyString())).thenReturn(unverified);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bidService.placeBid(auctionId, bidderId, request));

        assertEquals("KYC verification required to place a bid", exception.getMessage());
        verify(auctionRepository, never()).findByIdWithLock(any());
    }

    private UserProfile buildVerifiedProfile() {
        return UserProfile.newBuilder()
                .setUserId(UUID.randomUUID().toString())
                .setKycStatus("VERIFIED")
                .build();
    }

    private BidRequest buildBidRequest(BigDecimal amount, String idempotencyKey) {
        BidRequest request = new BidRequest();
        request.setAmount(amount);
        request.setIdempotencyKey(idempotencyKey);
        return request;
    }

    private Auction buildAuction(UUID auctionId, UUID sellerId) {
        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setTitle("Test Auction");
        auction.setSellerId(sellerId);
        auction.setStartingPrice(new BigDecimal("50.00"));
        auction.setBidIncrement(new BigDecimal("10.00"));
        auction.setStatus("PENDING");
        auction.setStartTime(LocalDateTime.now().minusHours(1));
        auction.setEndTime(LocalDateTime.now().plusDays(7));
        auction.setCurrency("USD");
        auction.setCreatedAt(LocalDateTime.now());
        auction.setUpdatedAt(LocalDateTime.now());
        return auction;
    }

    private Bid buildBid(UUID auctionId, UUID bidderId, BigDecimal amount) {
        Bid bid = new Bid();
        bid.setId(UUID.randomUUID());
        bid.setAuctionId(auctionId);
        bid.setBidderId(bidderId);
        bid.setAmount(amount);
        bid.setStatus("WINNING");
        bid.setCreatedAt(LocalDateTime.now());
        return bid;
    }
}
