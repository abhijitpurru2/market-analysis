package com.market.socialmedia;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SocialMediaIngestionService {

    private final KafkaTemplate<String, SocialPost> kafkaTemplate;
    private final Random random = new Random();

    @Value("${kafka.topics.social}")
    private String socialTopic;

    private static final List<String[]> DEMO_POSTS = List.of(
        new String[]{"twitter", "trader_joe", "AAPL looking strong, buying the dip! #AAPL", "AAPL"},
        new String[]{"reddit", "wallstreetbets", "TSLA to the moon 🚀 shorts getting squeezed", "TSLA"},
        new String[]{"twitter", "fin_analyst", "MSFT cloud division beats estimates. Bullish.", "MSFT"},
        new String[]{"reddit", "investing", "SPY ETF daily thread - what are your plays?", "SPY"},
        new String[]{"twitter", "hedge_desk", "GOOGL AI momentum continues, long-term hold", "GOOGL"}
    );

    @Scheduled(fixedDelay = 20000)
    public void ingestPosts() {
        DEMO_POSTS.forEach(p -> {
            SocialPost post = new SocialPost(
                UUID.randomUUID().toString(),
                p[0], p[1], p[2], p[3],
                random.nextInt(5000),
                random.nextInt(1000),
                Instant.now()
            );
            kafkaTemplate.send(socialTopic, post.getTicker(), post)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Failed to publish social post", ex);
                });
        });
    }
}
