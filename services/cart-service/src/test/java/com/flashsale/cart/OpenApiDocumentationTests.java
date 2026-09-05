package com.flashsale.cart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.cart.config.OpenApiConfig;
import com.flashsale.cart.api.CartController;
import com.flashsale.cart.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = OpenApiDocumentationTests.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"security.jwt.secret=swagger-test-only-key-with-more-than-32-characters", "security.jwt.issuer=swagger-test"})
class OpenApiDocumentationTests {
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(excludeName = {"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration", "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"})
    @Import({OpenApiConfig.class, CartController.class, GlobalExceptionHandler.class})
    static class TestApplication {}

    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper mapper;
    @MockitoBean com.flashsale.cart.service.CartService cartService;

    @Test
    void documentsActualEndpointsAndTheirAuthenticationRequirements() throws Exception {
        var response = http.getForEntity("/v3/api-docs", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode api = mapper.readTree(response.getBody());
        assertThat(api.path("info").path("title").asText()).isEqualTo("FlashSale — Cart Service");
        assertThat(api.path("paths").has("/cart/{userId}")).isTrue();
        assertThat(api.path("paths").has("/actuator/health")).isTrue();
        assertThat(api.at("/servers/0/url").asText()).isEqualTo("/");
        assertThat(api.at("/paths/~1cart~1{userId}~1items/post").isMissingNode()).isFalse();
        assertThat(api.at("/paths/~1cart~1{userId}~1items~1{sku}/put").isMissingNode()).isFalse();
    }

    @Test
    void servesSwaggerUiAndItsConfigurationWithoutLogin() {
        var ui = http.getForEntity("/swagger-ui/index.html", String.class);
        assertThat(ui.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ui.getBody()).contains("Swagger UI");
        var config = http.getForEntity("/v3/api-docs/swagger-config", String.class);
        assertThat(config.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(config.getBody()).doesNotContain("petstore.swagger.io");
    }

    @Test
    void servesYamlDefinitionWithoutLogin() {
        var yaml = http.getForEntity("/v3/api-docs.yaml", String.class);
        assertThat(yaml.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(yaml.getBody()).contains("openapi:", "/cart/{userId}:");
    }
}
