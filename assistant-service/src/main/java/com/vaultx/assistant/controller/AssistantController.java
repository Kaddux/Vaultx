package com.vaultx.assistant.controller;

import com.vaultx.assistant.dto.ChatRequest;
import com.vaultx.assistant.dto.ChatResponse;
import com.vaultx.assistant.service.AssistantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request,
                                             HttpServletRequest httpRequest) {
        String authorization = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        AssistantService.ChatResult result =
                assistantService.chat(request.getMessage(), request.getConversationId(), authorization);
        return ResponseEntity.ok(new ChatResponse(result.reply(), result.conversationId()));
    }

    @PostMapping("/conversations/{id}/reset")
    public ResponseEntity<Map<String, String>> reset(@PathVariable String id) {
        assistantService.reset(id);
        return ResponseEntity.ok(Map.of("status", "RESET"));
    }
}
