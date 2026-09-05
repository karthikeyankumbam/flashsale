package com.flashsale.api_gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.api_gateway.config.ApiDocsRoutes;
import com.flashsale.api_gateway.config.OpenApiConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = OpenApiProxyTests.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiProxyTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpServer UPSTREAM = startUpstream();

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({ApiDocsRoutes.class, OpenApiConfig.class})
    static class TestApplication {}

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry properties) {
        for (String service : List.of("auth", "catalog", "cart", "order", "inventory", "payment")) {
            properties.add("api-docs.services." + service, () -> "http://127.0.0.1:" + UPSTREAM.getAddress().getPort());
        }
    }

    @AfterAll static void closeUpstream() { UPSTREAM.stop(0); }

    @Autowired TestRestTemplate http;

    @ParameterizedTest
    @ValueSource(strings = {"auth", "catalog", "cart", "order", "inventory", "payment"})
    void eachDefinitionTargetsItsOwnGatewayPrefix(String service) throws Exception {
        var response = http.getForEntity("/openapi/" + service, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode definition = JSON.readTree(response.getBody());
        assertThat(definition.at("/servers/0/url").asText()).isEqualTo("/services/" + service);
        assertThat(definition.path("paths").has("/actuator/health")).isTrue();
        assertThat(response.getBody()).doesNotContain("wrong-host");
    }

    @ParameterizedTest
    @ValueSource(strings = {"auth", "catalog", "cart", "order", "inventory", "payment"})
    void healthRequestsReachTheSelectedServiceInsteadOfTheGateway(String service) throws Exception {
        JsonNode response = JSON.readTree(http.getForObject("/services/" + service + "/actuator/health", String.class));
        assertThat(response.path("path").asText()).isEqualTo("/actuator/health");
    }

    @Test
    void tryItOutPreservesMethodBodyHeadersAndQueryString() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("documentation-test-token");
        headers.set("X-User-Id", "U1001");
        headers.set("Idempotency-Key", "manual-001");
        headers.setContentType(MediaType.APPLICATION_JSON);
        var response = http.exchange("/services/order/orders?reason=manual", HttpMethod.POST,
                new HttpEntity<>("{\"currency\":\"INR\"}", headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode echo = JSON.readTree(response.getBody());
        assertThat(echo.path("path").asText()).isEqualTo("/orders");
        assertThat(echo.path("query").asText()).isEqualTo("reason=manual");
        assertThat(echo.path("method").asText()).isEqualTo("POST");
        assertThat(echo.path("authorization").asText()).isEqualTo("Bearer documentation-test-token");
        assertThat(echo.path("userId").asText()).isEqualTo("U1001");
        assertThat(echo.path("idempotencyKey").asText()).isEqualTo("manual-001");
        assertThat(JSON.readTree(echo.path("body").asText()).path("currency").asText()).isEqualTo("INR");
    }

    @Test
    void upstreamErrorsAreNotRewrittenIntoSuccessfulDefinitions() {
        var response = http.getForEntity("/openapi/catalog?unavailable=true", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).contains("upstream unavailable");
    }

    @Test
    void malformedUpstreamDefinitionReturnsBadGateway() {
        assertThat(http.getForEntity("/openapi/catalog?broken=true", String.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    private static HttpServer startUpstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                int status = 200;
                String body;
                if (exchange.getRequestURI().getPath().equals("/v3/api-docs")) {
                    if ("unavailable=true".equals(exchange.getRequestURI().getQuery())) {
                        status = 503;
                        body = "{\"message\":\"upstream unavailable\"}";
                    } else if ("broken=true".equals(exchange.getRequestURI().getQuery())) {
                        body = "invalid-json";
                    } else {
                        body = "{\"openapi\":\"3.0.1\",\"info\":{\"title\":\"Upstream\",\"version\":\"v1\"},"
                                + "\"servers\":[{\"url\":\"http://wrong-host\"}],\"paths\":{\"/actuator/health\":{}}}";
                    }
                } else {
                    var echo = JSON.createObjectNode();
                    echo.put("path", exchange.getRequestURI().getPath());
                    echo.put("query", exchange.getRequestURI().getQuery());
                    echo.put("method", exchange.getRequestMethod());
                    echo.put("authorization", exchange.getRequestHeaders().getFirst("Authorization"));
                    echo.put("userId", exchange.getRequestHeaders().getFirst("X-User-Id"));
                    echo.put("idempotencyKey", exchange.getRequestHeaders().getFirst("Idempotency-Key"));
                    echo.put("body", new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    body = JSON.writeValueAsString(echo);
                }
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
