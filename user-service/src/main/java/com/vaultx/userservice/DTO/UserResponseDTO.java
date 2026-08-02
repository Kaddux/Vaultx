package com.vaultx.userservice.DTO;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class UserResponseDTO {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String kycStatus;
    private BigDecimal userRating;
    private String role;
    private LocalDateTime createdAt;
}
