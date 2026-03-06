package com.flashsale.auth.api.dto;

public record AuthResponse(
        String accessToken,
        long expiresInSeconds
) {}