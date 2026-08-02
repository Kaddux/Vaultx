package com.vaultx.userservice.DTO;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
public class WalletResponse {
    private UUID id;
    private UUID userId;
    private BigDecimal balance;
    private BigDecimal reservedBalance;
    private BigDecimal availableBalance;
    private String currency;
}
