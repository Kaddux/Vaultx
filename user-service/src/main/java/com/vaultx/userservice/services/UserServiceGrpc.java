package com.vaultx.userservice.services;

import com.vaultx.userservice.model.Users;
import com.vaultx.userservice.model.Wallet;
import com.vaultx.userservice.repository.UserRepository;
import com.vaultx.userservice.repository.WalletRepository;
import com.vaultx.userservice.services.WalletService.WalletResult;
import com.vaultx.user.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class UserServiceGrpc extends com.vaultx.user.grpc.UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;

    @Override
    public void getUserProfile(GetUserProfileRequest request,
                               StreamObserver<UserProfile> responseObserver){
        try{
            UUID userId = UUID.fromString(request.getUserId());
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User Not Found"));

            UserProfile profile = UserProfile.newBuilder()
                    .setUserId(user.getId().toString())
                    .setUsername(user.getUsername())
                    .setEmail(user.getEmail())
                    .setFullName(user.getFullName() != null ? user.getFullName() : "")
                    .setKycStatus(user.getKycStatus())
                    .setUserRating(5.0)
                    .setRole(user.getRole())
                    .setIsActive(true)
                    .build();
            responseObserver.onNext(profile);
            responseObserver.onCompleted();
        }catch (Exception e) {
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND.withDescription
                            (e.getMessage()).asRuntimeException());
    }
}

    @Override
    public void getWalletBalance(GetWalletBalanceRequest request,
                                 StreamObserver<WalletBalance> responseObserver) {
        try {
            UUID userId = UUID.fromString(request.getUserId());
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Wallet not found: " + userId));

            WalletBalance balance = WalletBalance.newBuilder()
                    .setUserId(userId.toString())
                    .setBalance(wallet.getBalance().doubleValue())
                    .setReservedBalance(wallet.getReserveBalance().doubleValue())
                    .setCurrency(wallet.getCurrency())
                    .build();

            responseObserver.onNext(balance);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional
    public void updateWallet(UpdateWalletRequest request,
                             StreamObserver<com.vaultx.user.grpc.WalletResponse> responseObserver){
        try{
            UUID userId = UUID.fromString(request.getUserId());
            WalletResult result = walletService.walletMutation(
                    userId,
                    request.getTransactionType(),
                    request.getAmount(),
                    request.getIdempotencyKey(),
                    request.getDescription());

            com.vaultx.user.grpc.WalletResponse response =
                    com.vaultx.user.grpc.WalletResponse.newBuilder()
                            .setTransactionId(result.transactionId().toString())
                            .setNewBalance(result.newBalance().doubleValue())
                            .setNewReservedBalance(result.newReservedBalance().doubleValue())
                            .setStatus(result.status())
                            .setFailureReason(result.failureReason() == null ? "" : result.failureReason())
                            .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }catch (Exception e) {
            WalletResponse errorResponse = WalletResponse.newBuilder()
                    .setTransactionId("")
                    .setNewBalance(0)
                    .setNewReservedBalance(0)
                    .setStatus("FAILED")
                    .setFailureReason(e.getMessage())
                    .build();
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
    }
    }
}
