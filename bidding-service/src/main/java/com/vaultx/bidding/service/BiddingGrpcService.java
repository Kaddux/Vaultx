package com.vaultx.bidding.service;

import com.vaultx.bidding.grpc.AuctionDetails;
import com.vaultx.bidding.grpc.BiddingServiceGrpc;
import com.vaultx.bidding.grpc.GetAuctionDetailsRequest;
import com.vaultx.bidding.model.Auction;
import com.vaultx.bidding.repository.AuctionRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class BiddingGrpcService extends BiddingServiceGrpc.BiddingServiceImplBase {

    private final AuctionRepository auctionRepository;

    @Override
    public void getAuctionDetails(GetAuctionDetailsRequest request,
                                  StreamObserver<AuctionDetails> responseObserver) {
        try {
            UUID auctionId = UUID.fromString(request.getAuctionId());
            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new RuntimeException("Auction not found: " + auctionId));

            AuctionDetails details = AuctionDetails.newBuilder()
                    .setAuctionId(auction.getId().toString())
                    .setTitle(auction.getTitle())
                    .setSellerId(auction.getSellerId().toString())
                    .setCurrentBid(auction.getCurrentBid() != null
                            ? auction.getCurrentBid().doubleValue() : 0.0)
                    .setStartingPrice(auction.getStartingPrice().doubleValue())
                    .setReservePrice(auction.getReservePrice() != null
                            ? auction.getReservePrice().doubleValue() : 0.0)
                    .setStatus(auction.getStatus())
                    .setCurrency(auction.getCurrency())
                    .build();

            responseObserver.onNext(details);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}