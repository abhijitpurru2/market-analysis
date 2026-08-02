# market-analysis

Market Analysis is a microservices demo platform for ingesting market signals, processing them through Kafka, storing derived data in PostgreSQL, and exposing everything through a secured API gateway and chat UI.

## What changed

- Eureka/service discovery has been removed from the active runtime setup.
- Services now communicate through direct service URLs and Docker/Kubernetes service names.
- Kafka runs in KRaft mode, so ZooKeeper is no longer required.

## Architecture at a glance

### User-facing components

- `chat-frontend` (`3000`): React UI for asking market questions
- `api-gateway` (`8080`): single entry point, JWT-protected routing, rate limiting, CORS
- `chat-api-service` (`8085`): chat history + optional LLM integration backed by Redis

### Data producers

- `news-ingestion-service` (`8081`): publishes financial news events to Kafka
- `market-data-service` (`8082`): exposes quote APIs and publishes refreshed quote events to Kafka
- `social-media-service` (`8083`): publishes social sentiment posts to Kafka

### Data processors

- `sentiment-analysis-service` (`8086`): consumes news/social topics, scores text with a lightweight lexicon model, publishes sentiment events
- `market-data-processor` (`8087`): consumes quote and sentiment topics, writes processed records into PostgreSQL

### Platform services

- `kafka` (`9092`): event backbone
- `postgres` (`5432`): quote and sentiment persistence
- `redis` (`6379`): chat session storage and gateway rate limiting
- `prometheus` (`9090`) and `grafana` (`3001`): observability

## How the project works

1. The frontend sends requests to the API gateway.
2. The gateway validates JWTs, applies rate limits, and forwards requests to downstream services using configured URLs such as `http://news-ingestion:8081` and `http://chat-api:8085`.
3. The ingestion services and market data service publish events into Kafka topics:
   - `market.news.raw`
   - `market.social.posts`
   - `market.data.quotes`
4. `sentiment-analysis-service` consumes raw news and social posts, assigns a sentiment label/score, and publishes to `market.sentiment.scores`.
5. `market-data-processor` consumes quote and sentiment events and stores them in PostgreSQL tables such as `processed_quotes` and `sentiment_scores`.
6. `market-data-service` serves quote data over REST, while `chat-api-service` stores per-user chat history in Redis and can call an external LLM endpoint for responses.

## Request flow

### Chat flow

`chat-frontend` → `api-gateway` → `chat-api-service` → `Redis` → optional `LLM_ENDPOINT`

- `POST /api/chat/message` sends a prompt
- `GET /api/chat/history/{sessionId}` reads chat history
- `DELETE /api/chat/history/{sessionId}` clears chat history

### Market data flow

`POST /api/market/quotes/refresh` → `market-data-service` → Kafka `market.data.quotes` → `market-data-processor` → PostgreSQL

`GET /api/market/quotes/{ticker}` reads quote data from `market-data-service`

### Signal ingestion flow

- `POST /api/news/ingest` publishes demo news events
- `POST /api/social/ingest` publishes demo social posts
- both streams are analyzed by `sentiment-analysis-service`

## Security model

- All Spring services use the same `JWT_SECRET`.
- Gateway and backend APIs require JWT authentication for application endpoints.
- `/actuator/health` and `/actuator/info` remain open for health checks.
- Some write operations, such as ingestion triggers and quote refresh, are restricted to admin roles in the backend services.

## Running locally

### Prerequisites

- Java 21
- Maven
- Python 3.11
- Node.js 20 + npm
- Docker + Docker Compose

### 1) Start infrastructure only

```bash
docker compose up -d kafka postgres redis
```

### 2) Run Java services

```bash
cd /home/runner/work/market-analysis/market-analysis/api-gateway && mvn spring-boot:run
cd /home/runner/work/market-analysis/market-analysis/news-ingestion-service && mvn spring-boot:run
cd /home/runner/work/market-analysis/market-analysis/market-data-service && mvn spring-boot:run
cd /home/runner/work/market-analysis/market-analysis/social-media-service && mvn spring-boot:run
cd /home/runner/work/market-analysis/market-analysis/chat-api-service && mvn spring-boot:run
```

### 3) Run Python services

```bash
cd /home/runner/work/market-analysis/market-analysis/sentiment-analysis-service
python3.11 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8086

cd /home/runner/work/market-analysis/market-analysis/market-data-processor
python3.11 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8087
```

### 4) Run the frontend

```bash
cd /home/runner/work/market-analysis/market-analysis/chat-frontend
npm install
npm start
```

## Environment variables

Copy `/home/runner/work/market-analysis/market-analysis/.env.example` to `.env` before using Docker Compose.

Core values used across the stack:

- `JWT_SECRET`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `REDIS_PASSWORD`
- `GRAFANA_ADMIN_PASSWORD`
- `CORS_ALLOWED_ORIGINS`

Important service-specific overrides:

- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_REDIS_HOST`
- `SPRING_REDIS_PORT`
- `SPRING_REDIS_PASSWORD`
- `NEWS_INGESTION_URL`
- `MARKET_DATA_URL`
- `SOCIAL_MEDIA_URL`
- `CHAT_API_URL`
- `LLM_ENDPOINT`
- `LLM_MODEL`
- `KAFKA_BOOTSTRAP_SERVERS`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASS`
- `REACT_APP_API_BASE`

## Docker Compose

Start the full stack:

```bash
docker compose up --build
```

Main exposed ports:

- `3000` → frontend
- `3001` → Grafana
- `5432` → PostgreSQL
- `6379` → Redis
- `8080` → API gateway
- `8085` → chat API
- `9090` → Prometheus
- `9092` → Kafka

## Kubernetes and Helm

Raw manifests:

```bash
kubectl apply -f k8s/00-namespace.yml
kubectl apply -f k8s/01-infrastructure.yml
kubectl apply -f k8s/02-services.yml
```

Helm chart:

```bash
helm upgrade --install market-analysis /home/runner/work/market-analysis/market-analysis/helm/market-analysis \
  --namespace market-analysis \
  --create-namespace \
  --values /home/runner/work/market-analysis/market-analysis/helm/market-analysis/values.yaml
```

## Notes

- The React app defaults to `/api/chat` and the package proxy points at `http://api-gateway:8080`.
- The chat service defaults to `http://localhost:11434/api/generate` with model `llama3` if no LLM settings are supplied.
- The Python services expose `/health`; Spring services expose actuator health/info endpoints.
