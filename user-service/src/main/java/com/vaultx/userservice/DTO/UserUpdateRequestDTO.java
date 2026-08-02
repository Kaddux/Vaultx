package com.vaultx.userservice.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequestDTO {
    private String fullName;
    private String phone;
}