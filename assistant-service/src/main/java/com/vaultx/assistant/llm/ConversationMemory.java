package com.vaultx.assistant.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultx.assistant.config.AssistantProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stores each conversation's message history in Redis (per conversation id, with TTL).
 */
@Component
public class ConversationMemory {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final AssistantProperties props;

    public ConversationMemory(StringRedisTemplate redis, ObjectMapper objectMapper, AssistantProperties props) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    public List<Map<String, Object>> load(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) return new ArrayList<>();
        String raw = redis.opsForValue().get(key(conversationId));
        if (raw == null) return new ArrayList<>();
        try {
            return objectMapper.readValue(raw, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void save(String conversationId, List<Map<String, Object>> messages) {
        if (conversationId == null || conversationId.isBlank()) return;
        try {
            redis.opsForValue().set(key(conversationId), objectMapper.writeValueAsString(messages),
                    Duration.ofSeconds(props.getConversationTtlSeconds()));
        } catch (Exception ignored) {
            // memory is best-effort; failure should not break the chat
        }
    }

    public void clear(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) return;
        redis.delete(key(conversationId));
    }

    private String key(String conversationId) {
        return "assistant:convo:" + conversationId;
    }
}
