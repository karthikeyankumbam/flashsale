package com.flashsale.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdateProductRequest(
        @NotBlank String name,
        @NotBlank String category,
        @NotNull Long price,
        @NotBlank String currency,
        Boolean active,
        Map<String, Object> attributes
) {}