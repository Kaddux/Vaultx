package com.vaultx.assistant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.assistant")
@Getter
@Setter
public class AssistantProperties {
    private String ollamaBaseUrl = "http://localhost:11434";
    private String model = "qwen3:8b";
    private double temperature = 0.2;
    private int maxIterations = 4;
    private String gatewayUrl = "http://localhost:8080";
    private long conversationTtlSeconds = 3600;
}
