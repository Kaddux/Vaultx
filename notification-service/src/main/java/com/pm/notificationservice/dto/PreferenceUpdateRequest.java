package com.pm.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PreferenceUpdateRequest {

    @NotBlank
    private String channel;

    @NotNull
    private boolean enabled;
}
