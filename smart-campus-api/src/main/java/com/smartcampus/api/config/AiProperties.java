package com.smartcampus.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.groq")
public record AiProperties(
        String apiKey,
        String baseUrl,
        String model,
        Integer maxToolIterations,
        Integer requestTimeoutMs
) {}
