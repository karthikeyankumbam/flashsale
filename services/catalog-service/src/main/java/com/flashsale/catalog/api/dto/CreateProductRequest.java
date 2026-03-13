package com.flashsale.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateProductRequest(
        @NotBlank String sku,
        @NotBlank String name,
        @NotBlank String category,
        @NotNull Long price,
        @NotBlank String currency,
        Boolean active,
        Map<String, Object> attributes
) {}