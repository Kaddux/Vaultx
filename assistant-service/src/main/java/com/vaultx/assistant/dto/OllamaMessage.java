package com.vaultx.assistant.dto;

import java.util.List;
import java.util.Map;

/** Result shape returned by the LLM for a single completion. */
public record OllamaMessage(String role, String content, List<Map<String, Object>> toolCalls) {
}
