package com.flashsale.catalog.api.dto;

import com.flashsale.catalog.validation.CurrencyCode;
import com.flashsale.catalog.validation.ImageUrl;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;

public record UpdateProductRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 100) String category,
        @NotNull @Positive Long price,
        @NotBlank @CurrencyCode String currency,
        Boolean active,
        @Size(max = 30) Map<@NotBlank @Size(max = 80) @Pattern(regexp = "[^.$]+") String, Object> attributes,
        @Size(max = 5000) String description,
        @Size(max = 8) List<@NotBlank @Size(max = 2048) @ImageUrl String> images
) {}
