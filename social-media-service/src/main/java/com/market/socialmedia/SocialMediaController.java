package com.market.socialmedia;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialMediaController {

    private final SocialMediaIngestionService ingestionService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "social-media"));
    }

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> triggerIngestion() {
        ingestionService.ingestPosts();
        return ResponseEntity.ok(Map.of("message", "Social media ingestion triggered"));
    }
}
