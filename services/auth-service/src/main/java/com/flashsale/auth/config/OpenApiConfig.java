package com.flashsale.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI serviceOpenApi() {
        var healthBody = new Content().addMediaType("application/json", new MediaType()
                .schema(new ObjectSchema().addProperty("status", new StringSchema().example("UP"))));
        OpenAPI api = new OpenAPI()
                .info(new Info().title("FlashSale — Auth Service").version("v1")
                        .description("Register and log in, then copy accessToken into Authorize (paste the token without the Bearer prefix). Registration grants USER. Catalog management requires an ADMIN account."))
                .servers(List.of(new Server().url("/").description("This service")))
                .path("/actuator/health", new PathItem().get(new Operation()
                        .operationId("serviceHealth").summary("Check service health").tags(List.of("Health"))
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse().description("Healthy").content(healthBody))
                                .addApiResponse("503", new ApiResponse().description("A dependency is unavailable").content(healthBody)))));
        api.components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                .description("Paste the accessToken returned by Auth login; omit the Bearer prefix.")));
        return api;
    }
}
