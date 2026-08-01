package com.pm.notificationservice.grpc;

import com.vaultx.user.grpc.GetUserProfileRequest;
import com.vaultx.user.grpc.UserProfile;
import com.vaultx.user.grpc.UserServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class UserGrpcClient {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userStub;

    public UserProfile getUserProfile(String userId) {
        return userStub.getUserProfile(
                GetUserProfileRequest.newBuilder()
                        .setUserId(userId)
                        .build()
        );
    }
}
