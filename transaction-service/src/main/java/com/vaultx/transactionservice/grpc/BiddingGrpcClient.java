package com.vaultx.transactionservice.grpc;

import com.vaultx.bidding.grpc.AuctionDetails;
import com.vaultx.bidding.grpc.BiddingServiceGrpc;
import com.vaultx.bidding.grpc.GetAuctionDetailsRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class BiddingGrpcClient {

    @GrpcClient("bidding-service")
    private BiddingServiceGrpc.BiddingServiceBlockingStub biddingStub;

    public AuctionDetails getAuctionDetails(String auctionId) {
        return biddingStub.getAuctionDetails(
                GetAuctionDetailsRequest.newBuilder()
                        .setAuctionId(auctionId)
                        .build()
        );
    }
}
