package com.flashsale.auth.service;

import com.flashsale.auth.security.JwtService;
import com.flashsale.auth.user.Role;
import com.flashsale.auth.user.RoleRepository;
import com.flashsale.auth.user.User;
import com.flashsale.auth.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepo,
                       RoleRepository roleRepo,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public JwtService.TokenPair register(String email, String rawPassword) {
        if (userRepo.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User(email.toLowerCase(), passwordEncoder.encode(rawPassword));
        Role userRole = roleRepo.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("ROLE USER not found. Did Flyway run?"));
        user.addRole(userRole);

        userRepo.save(user);

        Set<String> roles = user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet());
        return jwtService.createAccessToken(user.getId(), user.getEmail(), roles);
    }

    @Transactional(readOnly = true)
    public JwtService.TokenPair login(String email, String rawPassword) {
        User user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("User is disabled");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        Set<String> roles = user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet());
        return jwtService.createAccessToken(user.getId(), user.getEmail(), roles);
    }
}