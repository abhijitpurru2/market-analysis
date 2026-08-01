package com.market.marketdata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDataService {

    private final MarketQuoteRepository quoteRepository;
    private final KafkaTemplate<String, MarketQuote> kafkaTemplate;
    private final Random random = new Random();

    @Value("${kafka.topics.market-data}")
    private String marketDataTopic;

    private static final List<String> TICKERS = List.of("AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "SPY", "QQQ");

    @Scheduled(fixedDelay = 10000)
    public void publishQuotes() {
        for (String ticker : TICKERS) {
            MarketQuote quote = generateQuote(ticker);
            quoteRepository.save(quote);
            kafkaTemplate.send(marketDataTopic, ticker, quote);
            log.debug("Published quote for {}: {}", ticker, quote.getPrice());
        }
    }

    private MarketQuote generateQuote(String ticker) {
        double basePrice = switch (ticker) {
            case "AAPL" -> 190.0;
            case "MSFT" -> 420.0;
            case "GOOGL" -> 175.0;
            case "AMZN" -> 185.0;
            case "TSLA" -> 250.0;
            case "SPY" -> 540.0;
            default -> 450.0;
        };
        double price = basePrice + (random.nextDouble() - 0.5) * 10;
        double change = (random.nextDouble() - 0.5) * 3;
        return new MarketQuote(null,
            ticker,
            BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP),
            BigDecimal.valueOf(change).setScale(2, RoundingMode.HALF_UP),
            (long) (random.nextInt(1_000_000) + 100_000),
            Instant.now());
    }

    public List<MarketQuote> getRecentQuotes(String ticker) {
        return quoteRepository.findTop10ByTickerOrderByTimestampDesc(ticker);
    }
}
