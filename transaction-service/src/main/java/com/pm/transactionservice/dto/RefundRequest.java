package com.pm.transactionservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class RefundRequest {

    @NotNull
    private UUID auctionId;
}
