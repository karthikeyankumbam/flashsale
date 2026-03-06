package com.flashsale.auth.security;

import java.util.Set;

public record AuthPrincipal(
        String userId,
        String email,
        Set<String> roles
) {}