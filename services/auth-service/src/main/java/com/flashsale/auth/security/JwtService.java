package com.flashsale.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        if (props.secret() == null || props.secret().length() < 32) {
            throw new IllegalArgumentException("security.jwt.secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenPair createAccessToken(UUID userId, String email, Set<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.accessTokenTtlMinutes() * 60);

        String jwt = Jwts.builder()
                .issuer(props.issuer())
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim("email", email)
                .claim("roles", roles)
                // ✅ jjwt 0.12.x style:
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        return new TokenPair(jwt, exp.getEpochSecond() - now.getEpochSecond());
    }

    public Jws<Claims> parse(String token) {
        // ✅ jjwt 0.12.x style:
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(token);
    }

    public record TokenPair(String token, long expiresInSeconds) {}
}