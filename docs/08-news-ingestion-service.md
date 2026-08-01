# News Ingestion Service

## Responsibilities

- Polls (or simulates) financial news sources on a 30-second schedule.
- Publishes `NewsArticle` events to the `market.news.raw` Kafka topic.
- Exposes a REST endpoint to trigger manual ingestion.

## Data Model

```mermaid
classDiagram
    class NewsArticle {
        String id
        String title
        String content
        String source
        String url
        Instant publishedAt
        String ticker
    }
```

## Kafka Producer

Messages are keyed by `ticker` symbol, enabling partition-level ordering per stock.

```mermaid
sequenceDiagram
    Scheduler->>NewsIngestionService: @Scheduled(fixedDelay=30s)
    NewsIngestionService->>ExternalAPI: fetch articles
    ExternalAPI-->>NewsIngestionService: articles[]
    loop for each article
        NewsIngestionService->>Kafka: send(market.news.raw, ticker, article)
    end
```

## Extension Points

To connect a real news API (e.g. Finnhub, NewsAPI), replace `fetchDemoArticles()` in `NewsIngestionService.java` with an `RestTemplate` or `WebClient` call.
