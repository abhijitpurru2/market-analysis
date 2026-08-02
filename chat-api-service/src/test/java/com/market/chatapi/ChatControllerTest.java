package com.market.chatapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    private Jwt mockJwt(String userId) {
        return Jwt.withTokenValue("test-token")
            .header("alg", "HS256")
            .subject(userId)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claims(claims -> claims.putAll(Map.of("sub", userId)))
            .build();
    }

    @Test
    void sendMessageShouldRejectBlankMessages() {
        ChatRequest request = new ChatRequest();
        request.setMessage("   ");

        ResponseEntity<ChatMessage> response = chatController.sendMessage(request, mockJwt("user1"));

        verify(chatService, never()).sendMessage(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void sendMessageShouldRejectNullRequest() {
        ResponseEntity<ChatMessage> response = chatController.sendMessage(null, mockJwt("user1"));

        verify(chatService, never()).sendMessage(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void clearHistoryShouldDeleteScopedSessionHistory() {
        ResponseEntity<Void> response = chatController.clearHistory("session-123", mockJwt("user1"));

        verify(chatService).clearHistory("user1:session-123");
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}

