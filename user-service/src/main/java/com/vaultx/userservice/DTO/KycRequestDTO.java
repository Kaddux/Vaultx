package com.vaultx.userservice.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KycRequestDTO {

    @NotBlank(message = "Document type is required")
    private String docType;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String address;

    private String documentRef;

    private String selfieRef;
}
