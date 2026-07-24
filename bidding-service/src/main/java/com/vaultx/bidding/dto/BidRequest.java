package com.vaultx.bidding.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class BidRequest {
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @DecimalMin("0.01")
    private BigDecimal maxAutoBid;

    @NotBlank
    private String idempotencyKey;
}
