package com.flashsale.auth.api.dto;

import java.util.Set;

public record MeResponse(
        String userId,
        String email,
        Set<String> roles
) {}