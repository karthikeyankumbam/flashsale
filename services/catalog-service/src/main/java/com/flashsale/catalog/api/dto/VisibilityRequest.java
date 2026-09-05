package com.flashsale.catalog.api.dto;

import jakarta.validation.constraints.NotNull;

public record VisibilityRequest(@NotNull Boolean active) {}
