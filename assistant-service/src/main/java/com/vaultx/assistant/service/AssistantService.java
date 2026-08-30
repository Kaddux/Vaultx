package com.vaultx.assistant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultx.assistant.config.AssistantProperties;
import com.vaultx.assistant.dto.OllamaMessage;
import com.vaultx.assistant.llm.ConversationMemory;
import com.vaultx.assistant.llm.OllamaClient;
import com.vaultx.assistant.tools.ToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chat orchestrator: runs the LLM, executes any tool calls against the Vaultx APIs,
 * and iterates until the model returns a final natural-language answer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final OllamaClient ollama;
    private final ToolExecutor toolExecutor;
    private final ConversationMemory memory;
    private final ObjectMapper objectMapper;
    private final AssistantProperties props;

    public ChatResult chat(String message, String conversationId, String authorization) {
        String convoId = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString() : conversationId;

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(system());
        messages.addAll(memory.load(convoId));
        messages.add(newMessage("user", message));

        List<Map<String, Object>> tools = toolExecutor.toolDefinitions();
        String reply = run(messages, authorization);

        // Keep history for follow-ups (exclude the system prompt).
        List<Map<String, Object>> history = new ArrayList<>(messages);
        history.add(newMessage("assistant", reply));
        memory.save(convoId, history.subList(1, history.size()));

        return new ChatResult(reply, convoId);
    }

    public void reset(String conversationId) {
        memory.clear(conversationId);
    }

    private String run(List<Map<String, Object>> messages, String authorization) {
        for (int i = 0; i < props.getMaxIterations(); i++) {
            OllamaMessage message = ollama.chat(messages, toolExecutor.toolDefinitions());
            if (message.toolCalls() == null || message.toolCalls().isEmpty()) {
                return message.content() == null ? "I couldn't find an answer." : message.content().trim();
            }

            // Record the assistant's tool calls, then execute each and append the results.
            messages.add(assistantToolCalls(message.toolCalls()));
            for (Map<String, Object> call : message.toolCalls()) {
                String callId = (String) call.get("id");
                String name = (String) ((Map<?, ?>) call.get("function")).get("name");
                String rawArgs = (String) ((Map<?, ?>) call.get("function")).get("arguments");
                Map<String, Object> args = parseArgs(rawArgs);

                Object result;
                try {
                    result = toolExecutor.execute(name, args, authorization);
                } catch (Exception e) {
                    result = Map.of("error", e.getMessage());
                }
                messages.add(toolMessage(callId, stringify(result)));
            }
        }
        return "I couldn't reach a final answer after several attempts. Please try rephrasing.";
    }

    private Map<String, Object> parseArgs(String rawArgs) {
        if (rawArgs == null || rawArgs.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(rawArgs, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String stringify(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    private Map<String, Object> system() {
        return newMessage("system",
                "You are Vaultx, a helpful auction assistant. You help users find auctions and answer "
                + "questions about them. You ONLY have read-only access and never place bids or change data. "
                + "All prices are in USD. Answer concisely and conversationally. When a question needs live data "
                + "(search, auction details, bids, wallet), use the provided tools and then summarize the results "
                + "for the user in natural language. Never invent data you did not retrieve.");
    }

    private Map<String, Object> newMessage(String role, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private Map<String, Object> assistantToolCalls(List<Map<String, Object>> toolCalls) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", "assistant");
        m.put("content", null);
        m.put("tool_calls", toolCalls);
        return m;
    }

    private Map<String, Object> toolMessage(String callId, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", "tool");
        m.put("tool_call_id", callId);
        m.put("content", content);
        return m;
    }

    public record ChatResult(String reply, String conversationId) {
    }
}
