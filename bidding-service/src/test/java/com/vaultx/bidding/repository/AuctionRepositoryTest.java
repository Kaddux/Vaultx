package com.vaultx.bidding.repository;

import com.vaultx.bidding.model.Auction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuctionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AuctionRepository auctionRepository;

    @Test
    void saveAndFindById_ShouldPersistAndRetrieve() {
        Auction auction = createTestAuction("PENDING");
        Auction saved = auctionRepository.save(auction);

        Optional<Auction> found = auctionRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Test Auction", found.get().getTitle());
        assertEquals(new BigDecimal("100.00"), found.get().getStartingPrice());
        assertEquals("PENDING", found.get().getStatus());
    }

    @Test
    void findByStatus_ShouldReturnMatchingAuctions() {
        Auction pending1 = createTestAuction("PENDING");
        pending1.setTitle("Pending Auction 1");
        Auction pending2 = createTestAuction("PENDING");
        pending2.setTitle("Pending Auction 2");
        Auction active = createTestAuction("ACTIVE");
        active.setTitle("Active Auction");

        auctionRepository.save(pending1);
        auctionRepository.save(pending2);
        auctionRepository.save(active);

        List<Auction> pending = auctionRepository.findByStatus("PENDING");
        List<Auction> activeList = auctionRepository.findByStatus("ACTIVE");
        List<Auction> ended = auctionRepository.findByStatus("ENDED");

        assertEquals(2, pending.size());
        assertEquals(1, activeList.size());
        assertEquals(0, ended.size());
    }

    @Test
    void findPendingToStart_ShouldReturnPendingAuctionsWithStartTimeBeforeNow() {
        Auction shouldStart = createTestAuction("PENDING");
        shouldStart.setStartTime(LocalDateTime.now().minusHours(2));
        shouldStart.setEndTime(LocalDateTime.now().plusDays(1));

        Auction shouldNotStart = createTestAuction("PENDING");
        shouldNotStart.setStartTime(LocalDateTime.now().plusHours(2));
        shouldNotStart.setEndTime(LocalDateTime.now().plusDays(2));

        Auction notPending = createTestAuction("ACTIVE");
        notPending.setStartTime(LocalDateTime.now().minusHours(1));
        notPending.setEndTime(LocalDateTime.now().plusDays(1));

        auctionRepository.save(shouldStart);
        auctionRepository.save(shouldNotStart);
        auctionRepository.save(notPending);

        List<Auction> results = auctionRepository.findPendingToStart(LocalDateTime.now());

        assertEquals(1, results.size());
        assertEquals("PENDING", results.get(0).getStatus());
        assertTrue(results.get(0).getStartTime().isBefore(LocalDateTime.now())
                || results.get(0).getStartTime().isEqual(LocalDateTime.now()));
    }

    @Test
    void findActiveToEnd_ShouldReturnActiveAuctionsWithEndTimeBeforeNow() {
        Auction shouldEnd = createTestAuction("ACTIVE");
        shouldEnd.setStartTime(LocalDateTime.now().minusDays(2));
        shouldEnd.setEndTime(LocalDateTime.now().minusHours(1));

        Auction shouldNotEnd = createTestAuction("ACTIVE");
        shouldNotEnd.setStartTime(LocalDateTime.now().minusDays(1));
        shouldNotEnd.setEndTime(LocalDateTime.now().plusHours(2));

        Auction notActive = createTestAuction("PENDING");
        notActive.setStartTime(LocalDateTime.now().minusDays(1));
        notActive.setEndTime(LocalDateTime.now().minusHours(1));

        auctionRepository.save(shouldEnd);
        auctionRepository.save(shouldNotEnd);
        auctionRepository.save(notActive);

        List<Auction> results = auctionRepository.findActiveToEnd(LocalDateTime.now());

        assertEquals(1, results.size());
        assertEquals("ACTIVE", results.get(0).getStatus());
        assertTrue(results.get(0).getEndTime().isBefore(LocalDateTime.now())
                || results.get(0).getEndTime().isEqual(LocalDateTime.now()));
    }

    @Test
    void findByIdWithLock_ShouldReturnAuctionWithOptimisticLock() {
        Auction auction = createTestAuction("ACTIVE");
        Auction saved = auctionRepository.save(auction);
        entityManager.flush();
        entityManager.clear();

        Optional<Auction> found = auctionRepository.findByIdWithLock(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertNotNull(found.get().getVersion());
    }

    @Test
    void findByIdWithLock_NotFound_ShouldReturnEmpty() {
        UUID nonExistentId = UUID.randomUUID();

        Optional<Auction> found = auctionRepository.findByIdWithLock(nonExistentId);

        assertTrue(found.isEmpty());
    }

    private Auction createTestAuction(String status) {
        Auction auction = new Auction();
        auction.setTitle("Test Auction");
        auction.setSellerId(UUID.randomUUID());
        auction.setStartingPrice(new BigDecimal("100.00"));
        auction.setBidIncrement(new BigDecimal("10.00"));
        auction.setStartTime(LocalDateTime.now().minusHours(1));
        auction.setEndTime(LocalDateTime.now().plusDays(1));
        auction.setStatus(status);
        return auction;
    }
}
