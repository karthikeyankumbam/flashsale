package com.flashsale.cart.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertCartItemRequest(
        @NotBlank String sku,
        @NotNull @Min(1) Integer qty
) {}