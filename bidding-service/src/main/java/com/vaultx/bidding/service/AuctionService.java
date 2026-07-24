package com.vaultx.bidding.service;

import com.vaultx.bidding.dto.AuctionRequest;
import com.vaultx.bidding.dto.AuctionResponse;
import com.vaultx.bidding.model.Auction;
import com.vaultx.bidding.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;

    @Transactional
    public AuctionResponse create(AuctionRequest request, UUID sellerId) {
        Auction auction = new Auction();
        auction.setId(UUID.randomUUID());
        auction.setTitle(request.getTitle());
        auction.setDescription(request.getDescription());
        auction.setSellerId(sellerId);
        auction.setStartingPrice(request.getStartingPrice());
        auction.setReservePrice(request.getReservePrice());
        auction.setBidIncrement(request.getBidIncrement());
        auction.setStartTime(request.getStartTime());
        auction.setEndTime(request.getEndTime());
        auction.setExtensionPeriodSeconds(request.getExtensionPeriodSeconds());
        auction.setCurrency(request.getCurrency());
        auction.setStatus("PENDING");
        return toResponse(auctionRepository.save(auction));
    }

    public AuctionResponse getById(UUID id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + id));
        return toResponse(auction);
    }

    public List<AuctionResponse> getAll(String statusFilter) {
        List<Auction> auctions = (statusFilter != null && !statusFilter.isBlank())
                ? auctionRepository.findByStatus(statusFilter.toUpperCase())
                : auctionRepository.findAll();
        return auctions.stream().map(this::toResponse).toList();
    }

    private AuctionResponse toResponse(Auction auction) {
        AuctionResponse r = new AuctionResponse();
        r.setId(auction.getId());
        r.setTitle(auction.getTitle());
        r.setDescription(auction.getDescription());
        r.setSellerId(auction.getSellerId());
        r.setStartingPrice(auction.getStartingPrice());
        r.setReservePrice(auction.getReservePrice());
        r.setCurrentBid(auction.getCurrentBid());
        r.setBidIncrement(auction.getBidIncrement());
        r.setStatus(auction.getStatus());
        r.setStartTime(auction.getStartTime());
        r.setEndTime(auction.getEndTime());
        r.setExtendedAt(auction.getExtendedAt());
        r.setExtensionPeriodSeconds(auction.getExtensionPeriodSeconds());
        r.setCurrency(auction.getCurrency());
        r.setCreatedAt(auction.getCreatedAt());
        return r;
    }
}
