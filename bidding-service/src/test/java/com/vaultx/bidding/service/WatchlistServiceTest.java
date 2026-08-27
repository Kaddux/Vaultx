package com.vaultx.bidding.service;

import com.vaultx.bidding.dto.WatchlistResponse;
import com.vaultx.bidding.model.Auction;
import com.vaultx.bidding.model.WatchlistEntry;
import com.vaultx.bidding.repository.AuctionRepository;
import com.vaultx.bidding.repository.WatchlistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @InjectMocks
    private WatchlistService watchlistService;

    @Test
    void add_newEntry_persistsAndReturnsTrue() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();

        when(auctionRepository.existsById(auctionId)).thenReturn(true);
        when(watchlistRepository.existsByUserIdAndAuctionId(userId, auctionId)).thenReturn(false);
        when(watchlistRepository.save(any(WatchlistEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        boolean added = watchlistService.add(userId, auctionId);

        assertTrue(added);
        verify(watchlistRepository).save(any(WatchlistEntry.class));
    }

    @Test
    void add_duplicateEntry_returnsFalseWithoutSaving() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();

        when(auctionRepository.existsById(auctionId)).thenReturn(true);
        when(watchlistRepository.existsByUserIdAndAuctionId(userId, auctionId)).thenReturn(true);

        boolean added = watchlistService.add(userId, auctionId);

        assertFalse(added);
        verify(watchlistRepository, never()).save(any(WatchlistEntry.class));
    }

    @Test
    void add_auctionNotFound_throwsRuntimeException() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();

        when(auctionRepository.existsById(auctionId)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> watchlistService.add(userId, auctionId));

        assertTrue(exception.getMessage().contains("Auction not found"));
    }

    @Test
    void remove_deletesEntry() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();

        watchlistService.remove(userId, auctionId);

        verify(watchlistRepository).deleteByUserIdAndAuctionId(userId, auctionId);
    }

    @Test
    void listForUser_returnsAuctionsJoined() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();

        WatchlistEntry entry = new WatchlistEntry();
        entry.setUserId(userId);
        entry.setAuctionId(auctionId);
        entry.setCreatedAt(LocalDateTime.now());

        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setTitle("Test Auction");
        auction.setSellerId(UUID.randomUUID());
        auction.setCurrentBid(new BigDecimal("150.00"));
        auction.setEndTime(LocalDateTime.now().plusHours(1));
        auction.setStatus("ACTIVE");

        when(watchlistRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(entry));
        when(auctionRepository.findAllById(List.of(auctionId))).thenReturn(List.of(auction));

        List<WatchlistResponse> responses = watchlistService.listForUser(userId);

        assertEquals(1, responses.size());
        WatchlistResponse r = responses.get(0);
        assertEquals(auctionId, r.getId());
        assertEquals("Test Auction", r.getTitle());
        assertEquals("ACTIVE", r.getStatus());
        assertEquals(new BigDecimal("150.00"), r.getCurrentBid());
    }

    @Test
    void listForUser_noEntries_returnsEmptyList() {
        UUID userId = UUID.randomUUID();
        when(watchlistRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of());

        assertEquals(0, watchlistService.listForUser(userId).size());
    }
}
