package com.flashsale.cart.config;

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
                .info(new Info().title("FlashSale — Cart Service").version("v1")
                        .description("Manage a shopping cart using the same userId throughout your test. The current cart API uses the userId path parameter and does not validate JWTs. Adding items does not reserve inventory."))
                .servers(List.of(new Server().url("/").description("This service")))
                .path("/actuator/health", new PathItem().get(new Operation()
                        .operationId("serviceHealth").summary("Check service health").tags(List.of("Health"))
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse().description("Healthy").content(healthBody))
                                .addApiResponse("503", new ApiResponse().description("A dependency is unavailable").content(healthBody)))));
        return api;
    }
}
