package com.flashsale.catalog.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public final class TestTokens {
    public static final String SECRET = "catalog-test-only-signing-key-with-more-than-sixty-four-characters-123456";
    public static final String ISSUER = "catalog-test-auth";
    private TestTokens() {}

    public static String admin() { return token(SECRET, ISSUER, "owner", List.of("USER", "ADMIN"), Instant.now().plusSeconds(300)); }
    public static String customer() { return token(SECRET, ISSUER, "customer", List.of("USER"), Instant.now().plusSeconds(300)); }

    public static String token(String secret, String issuer, String subject, Object roles, Instant expiration) {
        try {
            var claims = new JWTClaimsSet.Builder().issuer(issuer).subject(subject)
                    .issueTime(Date.from(Instant.now().minusSeconds(30))).claim("roles", roles);
            if (expiration != null) claims.expirationTime(Date.from(expiration));
            var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
            jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
