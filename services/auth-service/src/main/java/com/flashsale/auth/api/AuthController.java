package com.flashsale.auth.api;


import com.flashsale.auth.api.dto.*;
import com.flashsale.auth.security.AuthPrincipal;
import com.flashsale.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Set;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a customer account", description = "Creates a USER account and returns an access token.")
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        var token = authService.register(req.email(), req.password());
        return new AuthResponse(token.token(), token.expiresInSeconds());
    }

    @Operation(summary = "Log in and obtain an access token")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        var token = authService.login(req.email(), req.password());
        return new AuthResponse(token.token(), token.expiresInSeconds());
    }

    @Operation(summary = "View the current account and roles")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public MeResponse me(@Parameter(hidden = true) Authentication authentication) {
        var principal = (AuthPrincipal) authentication.getPrincipal();
        Set<String> roles = principal.roles();
        return new MeResponse(principal.userId(), principal.email(), roles);
    }
}
