package com.flashsale.catalog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {
    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) throw new IllegalArgumentException("JWT signing key must contain at least 32 bytes");
        var decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> requiredClaims = jwt -> {
            Object roles = jwt.getClaims().get("roles");
            boolean validRoles = roles == null || (roles instanceof Collection<?> values
                    && values.stream().allMatch(value -> value instanceof String));
            if (jwt.getExpiresAt() == null || jwt.getSubject() == null || jwt.getSubject().isBlank() || !validRoles) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Required token claims are invalid", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()), requiredClaims));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);

        var bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
        var bearerDenied = new BearerTokenAccessDeniedHandler();
        org.springframework.security.web.AuthenticationEntryPoint entryPoint = (request, response, ex) -> {
            bearerEntryPoint.commence(request, response, ex);
            writeProblem(mapper, response, 401, "Unauthorized", "A valid access token is required.");
        };
        org.springframework.security.web.access.AccessDeniedHandler denied = (request, response, ex) -> {
            bearerDenied.handle(request, response, ex);
            writeProblem(mapper, response, 403, "Forbidden", "Administrator access is required.");
        };

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        .requestMatchers("/products/admin/items", "/products/admin/items/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/products", "/products/**", "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/products", "/products/**").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint).accessDeniedHandler(denied))
                .oauth2ResourceServer(resource -> resource
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(entryPoint).accessDeniedHandler(denied));
        return http.build();
    }

    private static void writeProblem(ObjectMapper mapper, HttpServletResponse response,
                                     int status, String title, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
        problem.setTitle(title);
        mapper.writeValue(response.getOutputStream(), problem);
    }
}
