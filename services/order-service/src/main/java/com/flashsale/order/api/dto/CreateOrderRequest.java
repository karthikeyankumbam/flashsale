package com.flashsale.order.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateOrderRequest(
        @Schema(example = "INR") @NotBlank String currency,
        @NotEmpty @Valid List<Item> items
) {
    public record Item(
            @Schema(example = "IPHONE-16-128-BLK") @NotBlank String sku,
            @Schema(example = "1") @Positive Integer qty,
            @Schema(example = "79999") @Positive Long unitPrice
    ) {}
}
