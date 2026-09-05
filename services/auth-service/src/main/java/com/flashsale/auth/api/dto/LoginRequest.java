package com.flashsale.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "owner@example.com") @Email @NotBlank String email,
        @Schema(example = "use-your-own-password", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) @NotBlank String password
) {}
