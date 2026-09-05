package com.flashsale.cart.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertCartItemRequest(
        @Schema(example = "IPHONE-16-128-BLK") @NotBlank String sku,
        @Schema(example = "1") @NotNull @Min(1) Integer qty
) {}
