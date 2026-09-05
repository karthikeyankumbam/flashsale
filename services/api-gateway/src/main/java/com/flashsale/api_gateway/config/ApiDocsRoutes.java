package com.flashsale.api_gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Configuration
@EnableConfigurationProperties(ApiDocsProperties.class)
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
public class ApiDocsRoutes {
    @Bean
    RouteLocator documentationRoutes(RouteLocatorBuilder builder, ApiDocsProperties properties, ObjectMapper mapper) {
        var routes = builder.routes();
        properties.services().forEach((name, uri) -> {
            routes.route(name + "-openapi", route -> route.path("/openapi/" + name)
                    .filters(filters -> filters.setPath("/v3/api-docs")
                            .modifyResponseBody(String.class, String.class, (exchange, body) -> {
                                var status = exchange.getResponse().getStatusCode();
                                if (body == null || status == null || !status.is2xxSuccessful()) return Mono.justOrEmpty(body);
                                try {
                                    ObjectNode api = (ObjectNode) mapper.readTree(body);
                                    // Each definition targets its own upstream, including /actuator/health.
                                    api.putArray("servers").addObject().put("url", "/services/" + name)
                                            .put("description", "Through the API Gateway");
                                    return Mono.just(mapper.writeValueAsString(api));
                                } catch (Exception ex) {
                                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                            "Unable to read the service OpenAPI definition"));
                                }
                            }))
                    .uri(uri));
            routes.route(name + "-swagger-requests", route -> route.path("/services/" + name + "/**")
                    .filters(filters -> filters.stripPrefix(2)).uri(uri));
        });
        return routes.build();
    }
}
