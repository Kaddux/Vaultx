package com.vaultx.transactionservice.grpc;

import com.vaultx.user.grpc.GetWalletBalanceRequest;
import com.vaultx.user.grpc.GetWalletTransactionsRequest;
import com.vaultx.user.grpc.UpdateWalletRequest;
import com.vaultx.user.grpc.UserServiceGrpc;
import com.vaultx.user.grpc.WalletBalance;
import com.vaultx.user.grpc.WalletResponse;
import com.vaultx.user.grpc.WalletTransactionList;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class UserGrpcClient {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userStub;

    public WalletBalance getWalletBalance(String userId) {
        return userStub.getWalletBalance(
                GetWalletBalanceRequest.newBuilder()
                        .setUserId(userId)
                        .build()
        );
    }

    public WalletTransactionList getWalletTransactions(String userId) {
        return userStub.getWalletTransactions(
                GetWalletTransactionsRequest.newBuilder()
                        .setUserId(userId)
                        .build()
        );
    }

    public WalletResponse updateWallet(String userId, double amount,
                                        String transactionType,
                                        String idempotencyKey,
                                        String description) {
        UpdateWalletRequest request = UpdateWalletRequest.newBuilder()
                .setUserId(userId)
                .setAmount(amount)
                .setTransactionType(transactionType)
                .setIdempotencyKey(idempotencyKey)
                .setDescription(description)
                .build();
        return userStub.updateWallet(request);
    }
}
