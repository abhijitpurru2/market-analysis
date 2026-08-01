package com.market.chatapi;

import lombok.Data;

@Data
public class ChatRequest {
    private String sessionId;
    private String message;
}
