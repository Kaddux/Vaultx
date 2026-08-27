package com.vaultx.bidding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MediaUploadRequest {
    @NotBlank
    private String contentType;

    @NotBlank
    private String fileName;

    @NotNull
    @Positive
    private Long fileSizeBytes;
}
