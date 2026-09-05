package com.flashsale.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.auth.config.OpenApiConfig;
import com.flashsale.auth.api.AuthController;
import com.flashsale.auth.api.GlobalExceptionHandler;
import com.flashsale.auth.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = OpenApiDocumentationTests.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"security.jwt.secret=swagger-test-only-key-with-more-than-32-characters", "security.jwt.issuer=swagger-test"})
class OpenApiDocumentationTests {
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(excludeName = {"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration", "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration", "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"})
    @Import({OpenApiConfig.class, AuthController.class, GlobalExceptionHandler.class, SecurityConfig.class})
    static class TestApplication {}

    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper mapper;
    @MockBean com.flashsale.auth.service.AuthService authService;
    @MockBean com.flashsale.auth.security.JwtService jwtService;

    @Test
    void documentsActualEndpointsAndTheirAuthenticationRequirements() throws Exception {
        var response = http.getForEntity("/v3/api-docs", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode api = mapper.readTree(response.getBody());
        assertThat(api.path("info").path("title").asText()).isEqualTo("FlashSale — Auth Service");
        assertThat(api.path("paths").has("/auth/login")).isTrue();
        assertThat(api.path("paths").has("/actuator/health")).isTrue();
        assertThat(api.at("/servers/0/url").asText()).isEqualTo("/");
        assertThat(api.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        assertThat(api.at("/paths/~1auth~1me/get/security/0").has("bearerAuth")).isTrue();
        assertThat(api.at("/paths/~1auth~1login/post/security").isMissingNode()).isTrue();
        assertThat(api.at("/paths/~1auth~1register/post").isMissingNode()).isFalse();
        assertThat(api.at("/paths/~1auth~1me/get/parameters").toString()).doesNotContain("authentication");
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
        assertThat(yaml.getBody()).contains("openapi:", "/auth/login:");
    }
}
