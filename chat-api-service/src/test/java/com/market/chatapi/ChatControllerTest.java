package com.market.chatapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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

    @Test
    void sendMessageShouldRejectBlankMessages() {
        ChatRequest request = new ChatRequest();
        request.setMessage("   ");

        ResponseEntity<ChatMessage> response = chatController.sendMessage(request);

        verify(chatService, never()).sendMessage(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void sendMessageShouldRejectNullRequest() {
        ResponseEntity<ChatMessage> response = chatController.sendMessage(null);

        verify(chatService, never()).sendMessage(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void clearHistoryShouldDeleteSessionHistory() {
        ResponseEntity<Void> response = chatController.clearHistory("session-123");

        verify(chatService).clearHistory("session-123");
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
