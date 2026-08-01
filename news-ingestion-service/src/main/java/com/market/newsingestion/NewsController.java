package com.market.newsingestion;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsIngestionService newsIngestionService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "news-ingestion"));
    }

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> triggerIngestion() {
        newsIngestionService.ingestNews();
        return ResponseEntity.ok(Map.of("message", "Ingestion triggered"));
    }
}
