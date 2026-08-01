# API Reference

## Base URL

All requests go through the API Gateway: `http://localhost:8080`

---

## News Ingestion Service (`/api/news`)

### `GET /api/news/health`
Returns service health.

**Response**
```json
{ "status": "UP", "service": "news-ingestion" }
```

### `POST /api/news/ingest`
Triggers a manual ingestion cycle.

**Response**
```json
{ "message": "Ingestion triggered" }
```

---

## Market Data Service (`/api/market`)

### `GET /api/market/quotes/{ticker}`
Returns the 10 most recent quotes for a ticker.

**Parameters**
- `ticker` – Stock symbol (e.g. `AAPL`)

**Response**
```json
[
  {
    "id": "uuid",
    "ticker": "AAPL",
    "price": 191.32,
    "changePercent": 0.42,
    "volume": 4521000,
    "timestamp": "2026-08-01T12:00:00Z"
  }
]
```

### `POST /api/market/quotes/refresh`
Forces a new round of synthetic quotes to be generated and published.

---

## Chat API Service (`/api/chat`)

### `POST /api/chat/message`
Send a message and receive an LLM response.

**Request**
```json
{
  "sessionId": "optional-uuid",
  "message": "What is the AAPL sentiment today?"
}
```

**Response**
```json
{
  "sessionId": "uuid",
  "role": "assistant",
  "content": "AAPL sentiment is currently positive…",
  "timestamp": "2026-08-01T12:00:00Z"
}
```

### `GET /api/chat/history/{sessionId}`
Returns full conversation history for a session.

### `DELETE /api/chat/history/{sessionId}`
Clears conversation history for a session.

---

## Sentiment Analysis Service (direct, port 8086)

### `POST /analyze`
Analyse a piece of text.

**Request**
```json
{ "text": "AAPL looks great today!", "ticker": "AAPL" }
```

**Response**
```json
{ "ticker": "AAPL", "label": "POSITIVE", "score": 0.6 }
```

---

## Market Data Processor (direct, port 8087)

### `GET /stats/{ticker}`
Returns aggregated statistics for a ticker.

**Response**
```json
{
  "ticker": "AAPL",
  "avg_price": 191.25,
  "avg_sentiment": 0.35,
  "record_count": 1200
}
```
