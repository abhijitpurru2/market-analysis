package com.market.newsingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsIngestionService {

    private final KafkaTemplate<String, NewsArticle> kafkaTemplate;

    @Value("${kafka.topics.news}")
    private String newsTopic;

    @Scheduled(fixedDelay = 30000)
    public void ingestNews() {
        List<NewsArticle> articles = fetchDemoArticles();
        for (NewsArticle article : articles) {
            kafkaTemplate.send(newsTopic, article.getTicker(), article)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published news article: {}", article.getTitle());
                    } else {
                        log.error("Failed to publish article", ex);
                    }
                });
        }
    }

    private List<NewsArticle> fetchDemoArticles() {
        return List.of(
            new NewsArticle(UUID.randomUUID().toString(),
                "AAPL Reports Record Q3 Earnings",
                "Apple Inc. announced record third-quarter earnings driven by strong iPhone sales.",
                "Financial Times", "https://ft.com/aapl-q3", Instant.now(), "AAPL"),
            new NewsArticle(UUID.randomUUID().toString(),
                "Fed Signals Rate Hold Through Year-End",
                "The Federal Reserve signaled it will hold interest rates steady through year-end.",
                "Reuters", "https://reuters.com/fed-rates", Instant.now(), "SPY")
        );
    }
}
