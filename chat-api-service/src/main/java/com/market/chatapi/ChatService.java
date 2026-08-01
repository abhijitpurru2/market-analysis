package com.market.chatapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${chat.history.ttl-seconds}")
    private long ttlSeconds;

    @Value("${chat.history.max-messages}")
    private int maxMessages;

    @Value("${chat.llm.endpoint}")
    private String llmEndpoint;

    @Value("${chat.llm.model}")
    private String llmModel;

    private static final String SESSION_KEY_PREFIX = "chat:session:";

    public ChatMessage sendMessage(String sessionId, String userMessage) {
        String key = SESSION_KEY_PREFIX + sessionId;

        ChatMessage userMsg = new ChatMessage(sessionId, "user", userMessage, Instant.now());
        redisTemplate.opsForList().rightPush(key, userMsg);
        redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));

        String assistantResponse = generateResponse(userMessage);

        ChatMessage assistantMsg = new ChatMessage(sessionId, "assistant", assistantResponse, Instant.now());
        redisTemplate.opsForList().rightPush(key, assistantMsg);

        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > maxMessages * 2) {
            redisTemplate.opsForList().leftPop(key);
            redisTemplate.opsForList().leftPop(key);
        }

        return assistantMsg;
    }

    @SuppressWarnings("unchecked")
    public List<ChatMessage> getHistory(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) return Collections.emptyList();
        List<ChatMessage> messages = new ArrayList<>();
        for (Object obj : raw) {
            if (obj instanceof ChatMessage msg) {
                messages.add(msg);
            }
        }
        return messages;
    }

    private String generateResponse(String userMessage) {
        try {
            Map<String, Object> body = Map.of(
                "model", llmModel,
                "prompt", buildPrompt(userMessage),
                "stream", false
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(llmEndpoint, body, Map.class);
            if (response != null && response.containsKey("response")) {
                return (String) response.get("response");
            }
        } catch (Exception e) {
            log.warn("LLM call failed, returning demo response: {}", e.getMessage());
        }
        return generateDemoResponse(userMessage);
    }

    private String buildPrompt(String userMessage) {
        return """
            You are a professional financial market analyst assistant.
            Answer the following question concisely and accurately: %s
            """.formatted(userMessage);
    }

    private String generateDemoResponse(String userMessage) {
        String lower = userMessage.toLowerCase();
        if (lower.contains("aapl") || lower.contains("apple")) {
            return "AAPL is trading around $190, showing strength in the tech sector with strong iPhone demand.";
        } else if (lower.contains("tsla") || lower.contains("tesla")) {
            return "TSLA remains volatile. Recent delivery numbers beat estimates, supporting bullish momentum.";
        } else if (lower.contains("sentiment")) {
            return "Overall market sentiment is cautiously bullish. Tech stocks leading, energy lagging.";
        }
        return "Market analysis: The broader market shows mixed signals. Monitor the Fed's next move and earnings season.";
    }
}
