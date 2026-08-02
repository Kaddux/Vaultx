package com.vaultx.transactionservice.grpc;

import com.vaultx.transactionservice.model.Escrow;
import com.vaultx.transactionservice.repository.EscrowRepository;
import com.vaultx.transaction.grpc.GetPaymentStatusRequest;
import com.vaultx.transaction.grpc.PaymentStatus;
import com.vaultx.transaction.grpc.TransactionServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Optional;

@GrpcService
@RequiredArgsConstructor
public class TransactionServiceGrpcImpl
        extends TransactionServiceGrpc.TransactionServiceImplBase {

    private final EscrowRepository escrowRepository;

    @Override
    public void getPaymentStatus(GetPaymentStatusRequest request,
                                  StreamObserver<PaymentStatus> responseObserver) {
        try {
            String auctionId = request.getAuctionId();
            Optional<Escrow> escrow = escrowRepository
                    .findByAuctionId(java.util.UUID.fromString(auctionId));

            PaymentStatus.Builder builder = PaymentStatus.newBuilder()
                    .setStatus("NOT_FOUND");

            if (escrow.isPresent()) {
                Escrow e = escrow.get();
                builder.setPaymentIntentId(e.getId().toString())
                        .setStatus(e.getStatus())
                        .setAmount(e.getAmount().doubleValue())
                        .setCurrency("USD");
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

        } catch (Exception ex) {
            responseObserver.onError(ex);
        }
    }
}
