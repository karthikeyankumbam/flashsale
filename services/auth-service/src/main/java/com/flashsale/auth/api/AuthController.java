package com.flashsale.auth.api;


import com.flashsale.auth.api.dto.*;
import com.flashsale.auth.security.AuthPrincipal;
import com.flashsale.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        var token = authService.register(req.email(), req.password());
        return new AuthResponse(token.token(), token.expiresInSeconds());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        var token = authService.login(req.email(), req.password());
        return new AuthResponse(token.token(), token.expiresInSeconds());
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        var principal = (AuthPrincipal) authentication.getPrincipal();
        Set<String> roles = principal.roles();
        return new MeResponse(principal.userId(), principal.email(), roles);
    }
}
