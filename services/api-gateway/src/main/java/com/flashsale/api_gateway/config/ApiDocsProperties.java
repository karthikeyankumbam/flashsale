package com.flashsale.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.net.URI;
import java.util.Map;

@ConfigurationProperties(prefix = "api-docs")
public record ApiDocsProperties(Map<String, URI> services) {}
