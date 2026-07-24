package com.vaultx.bidding.grpc;

import com.vaultx.user.grpc.*;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserGrpcClient {
    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;
    public UserProfile getUserProfile(String id){
        GetUserProfileRequest request = GetUserProfileRequest.newBuilder()
                .setUserId(id).build();

        return userServiceStub.getUserProfile(request);
    }
    public WalletBalance getWalletBalance(String userId){
        GetWalletBalanceRequest request = GetWalletBalanceRequest.newBuilder()
                .setUserId(userId).build();

        return userServiceStub.getWalletBalance(request);
    }
}
