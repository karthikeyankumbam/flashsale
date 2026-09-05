package com.flashsale.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.flashsale.catalog.validation.CurrencyCode;
import com.flashsale.catalog.validation.ImageUrl;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;

public record CreateProductRequest(
        @Schema(example = "PHONE-DEMO-128") @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,127}",
                message = "must be 1–128 letters, digits, dots, underscores or hyphens, starting with a letter or digit") String sku,
        @Schema(example = "Demo Phone 128 GB") @NotBlank @Size(max = 200) String name,
        @Schema(example = "Phones") @NotBlank @Size(max = 100) String category,
        @Schema(example = "19999", description = "Whole currency units, matching the existing storefront.") @NotNull @Positive Long price,
        @Schema(example = "INR") @NotBlank @CurrencyCode String currency,
        @Schema(example = "false", description = "False creates a hidden product for review.") Boolean active,
        @Size(max = 30) Map<@NotBlank @Size(max = 80) @Pattern(regexp = "[^.$]+") String, Object> attributes,
        @Schema(example = "A compact phone with 128 GB storage.") @Size(max = 5000) String description,
        @Schema(example = "[]", description = "Up to eight HTTP or HTTPS image links.") @Size(max = 8) List<@NotBlank @Size(max = 2048) @ImageUrl String> images
) {}
