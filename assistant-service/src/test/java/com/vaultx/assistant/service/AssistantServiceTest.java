package com.vaultx.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultx.assistant.config.AssistantProperties;
import com.vaultx.assistant.dto.OllamaMessage;
import com.vaultx.assistant.llm.ConversationMemory;
import com.vaultx.assistant.llm.OllamaClient;
import com.vaultx.assistant.tools.ToolExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    @Mock private OllamaClient ollama;
    @Mock private ToolExecutor toolExecutor;
    @Mock private ConversationMemory memory;
    @Mock private AssistantProperties props;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private AssistantService service;

    @Test
    void chat_immediateAnswer_returnsReplyAndConversationId() {
        when(props.getMaxIterations()).thenReturn(4);
        when(ollama.chat(any(), any()))
                .thenReturn(new OllamaMessage("assistant", "Here are 12 matching auctions.", List.of()));

        AssistantService.ChatResult result = service.chat("Find cheap laptops", null, null);

        assertEquals("Here are 12 matching auctions.", result.reply());
        assertNotNull(result.conversationId());
    }

    @Test
    void chat_toolCall_thenAnswer_executesTool() {
        when(props.getMaxIterations()).thenReturn(4);

        Map<String, Object> call = Map.of(
                "id", "call_1",
                "type", "function",
                "function", Map.of("name", "list_auctions", "arguments", "{\"keywords\":\"laptop\",\"maxPrice\":70000}"));

        when(ollama.chat(any(), any()))
                .thenReturn(new OllamaMessage("assistant", null, List.of(call)))
                .thenReturn(new OllamaMessage("assistant", "Best match: ASUS ROG at $61,500.", List.of()));
        when(toolExecutor.execute(eq("list_auctions"), any(), any()))
                .thenReturn(Map.of("count", 12));

        AssistantService.ChatResult result = service.chat("Find me gaming laptops under 70000", null, null);

        assertEquals("Best match: ASUS ROG at $61,500.", result.reply());
        verify(toolExecutor).execute(eq("list_auctions"), any(), any());
        verify(ollama, times(2)).chat(any(), any());
    }

    @Test
    void chat_toolCall_error_isReturnedToLlm() {
        when(props.getMaxIterations()).thenReturn(4);

        Map<String, Object> call = Map.of(
                "id", "call_2",
                "type", "function",
                "function", Map.of("name", "get_wallet", "arguments", "{}"));

        when(ollama.chat(any(), any()))
                .thenReturn(new OllamaMessage("assistant", null, List.of(call)))
                .thenReturn(new OllamaMessage("assistant", "I couldn't read the wallet.", List.of()));
        when(toolExecutor.execute(eq("get_wallet"), any(), any())).thenThrow(new RuntimeException("401 Unauthorized"));

        AssistantService.ChatResult result = service.chat("What is my balance?", null, null);

        assertEquals("I couldn't read the wallet.", result.reply());
    }
}
