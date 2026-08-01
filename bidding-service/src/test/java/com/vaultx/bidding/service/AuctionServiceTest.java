package com.vaultx.bidding.service;

import com.vaultx.bidding.dto.AuctionRequest;
import com.vaultx.bidding.dto.AuctionResponse;
import com.vaultx.bidding.model.Auction;
import com.vaultx.bidding.model.OutboxEvent;
import com.vaultx.bidding.repository.AuctionRepository;
import com.vaultx.bidding.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuctionService auctionService;

    @Test
    void create_success_createsAuctionSavesItCreatesOutboxEventReturnsResponse() throws Exception {
        UUID sellerId = UUID.randomUUID();
        AuctionRequest request = buildAuctionRequest();

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        AuctionResponse response = auctionService.create(request, sellerId);

        assertNotNull(response);
        assertEquals(request.getTitle(), response.getTitle());
        assertEquals(request.getDescription(), response.getDescription());
        assertEquals(sellerId, response.getSellerId());
        assertEquals(request.getStartingPrice(), response.getStartingPrice());
        assertEquals(request.getReservePrice(), response.getReservePrice());
        assertEquals(request.getBidIncrement(), response.getBidIncrement());
        assertEquals(request.getStartTime(), response.getStartTime());
        assertEquals(request.getEndTime(), response.getEndTime());
        assertEquals(request.getExtensionPeriodSeconds(), response.getExtensionPeriodSeconds());
        assertEquals(request.getCurrency(), response.getCurrency());

        verify(auctionRepository).save(any(Auction.class));
        verify(outboxEventRepository).save(any(OutboxEvent.class));
        verify(objectMapper).writeValueAsString(any());
    }

    @Test
    void create_success_verifiesAuctionStatusPendingSellerIdSetFieldsMappedCorrectly() throws Exception {
        UUID sellerId = UUID.randomUUID();
        AuctionRequest request = buildAuctionRequest();

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        auctionService.create(request, sellerId);

        ArgumentCaptor<Auction> auctionCaptor = ArgumentCaptor.forClass(Auction.class);
        verify(auctionRepository).save(auctionCaptor.capture());
        Auction captured = auctionCaptor.getValue();

        assertEquals("PENDING", captured.getStatus());
        assertEquals(sellerId, captured.getSellerId());
        assertEquals(request.getTitle(), captured.getTitle());
        assertEquals(request.getDescription(), captured.getDescription());
        assertEquals(request.getStartingPrice(), captured.getStartingPrice());
        assertEquals(request.getReservePrice(), captured.getReservePrice());
        assertEquals(request.getBidIncrement(), captured.getBidIncrement());
        assertEquals(request.getStartTime(), captured.getStartTime());
        assertEquals(request.getEndTime(), captured.getEndTime());
        assertEquals(request.getExtensionPeriodSeconds(), captured.getExtensionPeriodSeconds());
        assertEquals(request.getCurrency(), captured.getCurrency());
        assertNotNull(captured.getId());
    }

    @Test
    void getById_success_auctionFound() {
        UUID auctionId = UUID.randomUUID();
        Auction auction = buildAuction(auctionId);
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        AuctionResponse response = auctionService.getById(auctionId);

        assertNotNull(response);
        assertEquals(auctionId, response.getId());
        assertEquals(auction.getTitle(), response.getTitle());
        assertEquals(auction.getStatus(), response.getStatus());
    }

    @Test
    void getById_error_notFound_throwsRuntimeException() {
        UUID auctionId = UUID.randomUUID();
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> auctionService.getById(auctionId));

        assertTrue(exception.getMessage().contains("Auction not found"));
        assertTrue(exception.getMessage().contains(auctionId.toString()));
        verify(auctionRepository).findById(auctionId);
    }

    @Test
    void getAll_success_withStatusFilter_callsFindByStatus() {
        String statusFilter = "ACTIVE";
        Auction auction = buildAuction(UUID.randomUUID());
        auction.setStatus("ACTIVE");
        when(auctionRepository.findByStatus(statusFilter)).thenReturn(List.of(auction));

        List<AuctionResponse> responses = auctionService.getAll(statusFilter);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(auction.getId(), responses.get(0).getId());
        verify(auctionRepository).findByStatus(statusFilter);
        verify(auctionRepository, never()).findAll();
    }

    @Test
    void getAll_success_nullFilter_callsFindAll() {
        Auction auction = buildAuction(UUID.randomUUID());
        when(auctionRepository.findAll()).thenReturn(List.of(auction));

        List<AuctionResponse> responses = auctionService.getAll(null);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(auctionRepository).findAll();
        verify(auctionRepository, never()).findByStatus(anyString());
    }

    @Test
    void getAll_success_blankFilter_callsFindAll() {
        Auction auction = buildAuction(UUID.randomUUID());
        when(auctionRepository.findAll()).thenReturn(List.of(auction));

        List<AuctionResponse> responses = auctionService.getAll("   ");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(auctionRepository).findAll();
        verify(auctionRepository, never()).findByStatus(anyString());
    }

    private AuctionRequest buildAuctionRequest() {
        AuctionRequest request = new AuctionRequest();
        request.setTitle("Test Auction");
        request.setDescription("Test Description");
        request.setStartingPrice(new BigDecimal("100.00"));
        request.setReservePrice(new BigDecimal("200.00"));
        request.setBidIncrement(new BigDecimal("10.00"));
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(7));
        request.setExtensionPeriodSeconds(120);
        request.setCurrency("USD");
        return request;
    }

    private Auction buildAuction(UUID id) {
        Auction auction = new Auction();
        auction.setId(id);
        auction.setTitle("Test Auction");
        auction.setDescription("Test Description");
        auction.setSellerId(UUID.randomUUID());
        auction.setStartingPrice(new BigDecimal("100.00"));
        auction.setReservePrice(new BigDecimal("200.00"));
        auction.setBidIncrement(new BigDecimal("10.00"));
        auction.setStatus("PENDING");
        auction.setStartTime(LocalDateTime.now().plusDays(1));
        auction.setEndTime(LocalDateTime.now().plusDays(7));
        auction.setExtensionPeriodSeconds(120);
        auction.setCurrency("USD");
        auction.setCreatedAt(LocalDateTime.now());
        auction.setUpdatedAt(LocalDateTime.now());
        return auction;
    }
}
