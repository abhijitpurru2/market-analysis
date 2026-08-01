package com.market.chatapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    @Test
    void clearHistoryShouldDeleteSessionHistory() {
        ResponseEntity<Void> response = chatController.clearHistory("session-123");

        verify(chatService).clearHistory("session-123");
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
