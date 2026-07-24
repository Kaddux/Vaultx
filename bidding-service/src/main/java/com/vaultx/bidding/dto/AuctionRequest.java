package com.vaultx.bidding.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class AuctionRequest {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal startingPrice;

    @DecimalMin("0.01")
    private BigDecimal reservePrice;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal bidIncrement;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    private int extensionPeriodSeconds = 120;

    private String currency = "USD";
}
