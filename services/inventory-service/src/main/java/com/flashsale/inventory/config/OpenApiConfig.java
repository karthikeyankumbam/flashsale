package com.flashsale.inventory.config;

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
                .info(new Info().title("FlashSale — Inventory Service").version("v1")
                        .description("Read stock by SKU. Reservations are triggered by flashsale.order.events and publish results to flashsale.inventory.events. Swagger tests the HTTP inventory read; create an order to exercise reservation."))
                .servers(List.of(new Server().url("/").description("This service")))
                .path("/actuator/health", new PathItem().get(new Operation()
                        .operationId("serviceHealth").summary("Check service health").tags(List.of("Health"))
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse().description("Healthy").content(healthBody))
                                .addApiResponse("503", new ApiResponse().description("A dependency is unavailable").content(healthBody)))));
        return api;
    }
}
