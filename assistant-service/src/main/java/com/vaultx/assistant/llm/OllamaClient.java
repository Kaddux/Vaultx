package com.vaultx.assistant.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultx.assistant.config.AssistantProperties;
import com.vaultx.assistant.dto.OllamaMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin client for the local Ollama OpenAI-compatible endpoint
 * (POST /v1/chat/completions) with tool/function-calling support.
 */
@Component
public class OllamaClient {

    private final RestClient restClient;
    private final AssistantProperties props;
    private final ObjectMapper objectMapper;

    @Autowired
    public OllamaClient(RestClient.Builder builder, AssistantProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.restClient = builder.baseUrl(props.getOllamaBaseUrl()).build();
    }

    /**
     * Sends a chat completion. `tools` is a list of OpenAI-style function definitions.
     * Returns the assistant message (which may contain toolCalls).
     */
    public OllamaMessage chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getModel());
        body.put("messages", messages);
        body.put("temperature", props.getTemperature());
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }

        String response = restClient.post()
                .uri("/v1/chat/completions")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        return parseMessage(response);
    }

    private OllamaMessage parseMessage(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode message = root.path("choices").path(0).path("message");
            String role = message.path("role").asText("assistant");
            String content = message.hasNonNull("content") ? message.path("content").asText() : null;

            List<Map<String, Object>> toolCalls = new ArrayList<>();
            if (message.has("tool_calls")) {
                for (JsonNode tc : message.path("tool_calls")) {
                    Map<String, Object> call = new LinkedHashMap<>();
                    call.put("id", tc.path("id").asText());
                    call.put("type", tc.path("type").asText());
                    Map<String, Object> fn = new LinkedHashMap<>();
                    fn.put("name", tc.path("function").path("name").asText());
                    fn.put("arguments", tc.path("function").path("arguments").asText());
                    call.put("function", fn);
                    toolCalls.add(call);
                }
            }
            return new OllamaMessage(role, content, toolCalls);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Ollama response: " + json, e);
        }
    }
}
