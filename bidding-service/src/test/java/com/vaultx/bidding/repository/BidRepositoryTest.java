package com.vaultx.bidding.repository;

import com.vaultx.bidding.model.Bid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BidRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BidRepository bidRepository;

    private final UUID auctionId = UUID.randomUUID();
    private final UUID bidderId = UUID.randomUUID();

    @Test
    void saveAndFindById_ShouldPersistAndRetrieve() {
        Bid bid = createBid(new BigDecimal("150.00"), "key-1");
        Bid saved = bidRepository.save(bid);

        Optional<Bid> found = bidRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(new BigDecimal("150.00"), found.get().getAmount());
        assertEquals("ACTIVE", found.get().getStatus());
    }

    @Test
    void findByIdempotencyKey_WhenExists_ShouldReturnBid() {
        String key = "idem-" + UUID.randomUUID();
        Bid bid = createBid(new BigDecimal("200.00"), key);
        bidRepository.save(bid);

        Optional<Bid> found = bidRepository.findByIdempotencyKey(key);

        assertTrue(found.isPresent());
        assertEquals(key, found.get().getIdempotencyKey());
        assertEquals(new BigDecimal("200.00"), found.get().getAmount());
    }

    @Test
    void findByIdempotencyKey_WhenNotExists_ShouldReturnEmpty() {
        Optional<Bid> found = bidRepository.findByIdempotencyKey("non-existent-key");

        assertTrue(found.isEmpty());
    }

    @Test
    void findByAuctionIdOrderByCreatedAtDesc_ShouldReturnBidsOrderedByCreatedAtDesc() throws Exception {
        Bid bid1 = createBid(new BigDecimal("100.00"), "key-" + UUID.randomUUID());
        bidRepository.save(bid1);
        Thread.sleep(5);
        Bid bid2 = createBid(new BigDecimal("200.00"), "key-" + UUID.randomUUID());
        bidRepository.save(bid2);
        Thread.sleep(5);
        Bid bid3 = createBid(new BigDecimal("300.00"), "key-" + UUID.randomUUID());
        bidRepository.save(bid3);
        entityManager.flush();

        List<Bid> results = bidRepository.findByAuctionIdOrderByCreatedAtDesc(auctionId);

        assertEquals(3, results.size());
        assertEquals(new BigDecimal("300.00"), results.get(0).getAmount());
        assertEquals(new BigDecimal("200.00"), results.get(1).getAmount());
        assertEquals(new BigDecimal("100.00"), results.get(2).getAmount());
    }

    @Test
    void findByAuctionIdAndBidderIdOrderByCreatedAtDesc_ShouldReturnUserBidsForAuction() throws Exception {
        Bid myBid1 = createBid(new BigDecimal("100.00"), "key-" + UUID.randomUUID());
        Bid myBid2 = createBid(new BigDecimal("200.00"), "key-" + UUID.randomUUID());

        UUID otherBidderId = UUID.randomUUID();
        Bid otherBid = new Bid();
        otherBid.setAuctionId(auctionId);
        otherBid.setBidderId(otherBidderId);
        otherBid.setAmount(new BigDecimal("300.00"));
        otherBid.setStatus("ACTIVE");
        otherBid.setIdempotencyKey("key-" + UUID.randomUUID());

        bidRepository.save(myBid1);
        Thread.sleep(5);
        bidRepository.save(otherBid);
        Thread.sleep(5);
        bidRepository.save(myBid2);
        entityManager.flush();

        List<Bid> results = bidRepository.findByAuctionIdAndBidderIdOrderByCreatedAtDesc(auctionId, bidderId);

        assertEquals(2, results.size());
        assertEquals(new BigDecimal("200.00"), results.get(0).getAmount());
        assertEquals(new BigDecimal("100.00"), results.get(1).getAmount());
        results.forEach(b -> assertEquals(bidderId, b.getBidderId()));
    }

    @Test
    void findTopByAuctionIdOrderByAmountDesc_ShouldReturnHighestBid() {
        Bid lowBid = createBid(new BigDecimal("100.00"), "key-" + UUID.randomUUID());
        Bid highBid = createBid(new BigDecimal("500.00"), "key-" + UUID.randomUUID());
        Bid midBid = createBid(new BigDecimal("300.00"), "key-" + UUID.randomUUID());

        bidRepository.save(lowBid);
        bidRepository.save(highBid);
        bidRepository.save(midBid);
        entityManager.flush();

        Optional<Bid> topBid = bidRepository.findTopByAuctionIdOrderByAmountDesc(auctionId);

        assertTrue(topBid.isPresent());
        assertEquals(new BigDecimal("500.00"), topBid.get().getAmount());
    }

    @Test
    void markOutbidByAuction_ShouldChangeWinningToOutbid() {
        Bid winningBid = createBid(new BigDecimal("200.00"), "key-" + UUID.randomUUID());
        winningBid.setStatus("WINNING");
        bidRepository.save(winningBid);

        Bid activeBid = createBid(new BigDecimal("100.00"), "key-" + UUID.randomUUID());
        activeBid.setStatus("ACTIVE");
        bidRepository.save(activeBid);

        entityManager.flush();

        int updated = bidRepository.markOutbidByAuction(auctionId);
        assertEquals(1, updated);

        entityManager.clear();

        Bid updatedWinningBid = bidRepository.findById(winningBid.getId()).orElseThrow();
        assertEquals("OUTBID", updatedWinningBid.getStatus());

        Bid unchangedActiveBid = bidRepository.findById(activeBid.getId()).orElseThrow();
        assertEquals("ACTIVE", unchangedActiveBid.getStatus());
    }

    @Test
    void markOutbidByAuction_WhenNoWinningBids_ShouldReturnZero() {
        Bid activeBid = createBid(new BigDecimal("100.00"), "key-" + UUID.randomUUID());
        activeBid.setStatus("ACTIVE");
        bidRepository.save(activeBid);
        entityManager.flush();

        int updated = bidRepository.markOutbidByAuction(auctionId);

        assertEquals(0, updated);
    }

    private Bid createBid(BigDecimal amount, String idempotencyKey) {
        Bid bid = new Bid();
        bid.setAuctionId(auctionId);
        bid.setBidderId(bidderId);
        bid.setAmount(amount);
        bid.setStatus("ACTIVE");
        bid.setIdempotencyKey(idempotencyKey);
        return bid;
    }
}
