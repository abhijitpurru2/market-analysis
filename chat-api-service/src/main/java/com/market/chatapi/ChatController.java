package com.market.chatapi;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/message")
    public ResponseEntity<ChatMessage> sendMessage(@RequestBody ChatRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            request.setSessionId(UUID.randomUUID().toString());
        }
        String scopedSessionId = jwt.getSubject() + ":" + request.getSessionId();
        ChatMessage response = chatService.sendMessage(scopedSessionId, request.getMessage());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getHistory(@PathVariable String sessionId,
            @AuthenticationPrincipal Jwt jwt) {
        String scopedSessionId = jwt.getSubject() + ":" + sessionId;
        return ResponseEntity.ok(chatService.getHistory(scopedSessionId));
    }

    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<Void> clearHistory(@PathVariable String sessionId,
            @AuthenticationPrincipal Jwt jwt) {
        String scopedSessionId = jwt.getSubject() + ":" + sessionId;
        chatService.clearHistory(scopedSessionId);
        return ResponseEntity.noContent().build();
    }
}

