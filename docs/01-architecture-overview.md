# Architecture Overview

## System Design

The market-analysis platform is an enterprise-style microservices system built to ingest, process, and analyse financial market data in near real-time, with an LLM-powered chat interface for user queries.

```mermaid
flowchart LR
    USER[Analyst or End User]
    LLM[External LLM Endpoint]

    subgraph Experience Layer
        FE[Chat Frontend<br/>React]
        GW[API Gateway<br/>Spring Cloud Gateway]
    end

    subgraph Core Application Layer
        SR[Service Registry<br/>Eureka]
        CA[Chat API Service]
        NI[News Ingestion Service]
        MD[Market Data Service]
        SM[Social Media Service]
    end

    subgraph Streaming and Processing Layer
        K[(Kafka)]
        SA[Sentiment Analysis Service]
        MDP[Market Data Processor]
    end

    subgraph Data and Operations Layer
        PG[(PostgreSQL)]
        RD[(Redis)]
        PR[Prometheus]
        GR[Grafana]
    end

    USER --> FE --> GW
    GW --> CA
    GW --> NI
    GW --> MD
    GW --> SM
    GW -. service discovery .-> SR
    CA -. service discovery .-> SR
    NI -. service discovery .-> SR
    MD -. service discovery .-> SR
    SM -. service discovery .-> SR
    CA -. optional inference .-> LLM
    NI --> K
    MD --> K
    SM --> K
    K --> SA
    SA --> K
    K --> MDP
    MD --> PG
    MDP --> PG
    CA --> RD
    PR --> GR
```

## End-to-End Runtime Flow

```mermaid
flowchart TB
    subgraph Client Requests
        USER[User]
        FE[Frontend]
        GW[API Gateway]
    end

    subgraph Synchronous APIs
        CHAT[Chat API]
        NEWS[News Ingestion API]
        MARKET[Market Data API]
        SOCIAL[Social Media API]
    end

    subgraph Event Backbone
        TOPICS[(Kafka Topics)]
        SENT[Sentiment Analysis]
        PROC[Market Data Processor]
    end

    subgraph Persistence
        DB[(PostgreSQL)]
        CACHE[(Redis)]
    end

    USER --> FE --> GW
    GW --> CHAT
    GW --> NEWS
    GW --> MARKET
    GW --> SOCIAL
    CHAT --> CACHE
    NEWS --> TOPICS
    MARKET --> TOPICS
    SOCIAL --> TOPICS
    TOPICS --> SENT
    SENT --> TOPICS
    TOPICS --> PROC
    PROC --> DB
    MARKET --> DB
    DB --> MARKET
    MARKET --> GW
    CHAT --> GW
```

## Platform and Deployment View

```mermaid
flowchart LR
    subgraph Runtime Environments
        DEV[Local Docker Compose]
        K8S[Kubernetes / Helm]
    end

    subgraph Shared Platform Components
        K[(Kafka<br/>KRaft mode)]
        PG[(PostgreSQL)]
        RD[(Redis)]
        PROM[Prometheus]
        GRAF[Grafana]
    end

    subgraph Application Services
        FE[chat-frontend]
        GW[api-gateway]
        SR[service-registry]
        NI[news-ingestion-service]
        MD[market-data-service]
        SM[social-media-service]
        CA[chat-api-service]
        SA[sentiment-analysis-service]
        MDP[market-data-processor]
    end

    DEV --> FE
    DEV --> GW
    DEV --> SR
    DEV --> NI
    DEV --> MD
    DEV --> SM
    DEV --> CA
    DEV --> SA
    DEV --> MDP

    K8S --> FE
    K8S --> GW
    K8S --> SR
    K8S --> NI
    K8S --> MD
    K8S --> SM
    K8S --> CA
    K8S --> SA
    K8S --> MDP

    NI --> K
    MD --> K
    SM --> K
    SA --> K
    MDP --> PG
    MD --> PG
    CA --> RD
    GW -. metrics .-> PROM
    NI -. metrics .-> PROM
    MD -. metrics .-> PROM
    SM -. metrics .-> PROM
    CA -. metrics .-> PROM
    PROM --> GRAF
```

## Service Catalogue

| Service | Port | Technology | Role |
|---------|------|------------|------|
| `service-registry` | 8761 | Spring Cloud Eureka | Service discovery |
| `api-gateway` | 8080 | Spring Cloud Gateway | Routing, load balancing |
| `news-ingestion-service` | 8081 | Spring Boot + Kafka | Financial news producer |
| `market-data-service` | 8082 | Spring Boot + JPA | Quote data REST + Kafka |
| `social-media-service` | 8083 | Spring Boot + Kafka | Social signals producer |
| `chat-api-service` | 8085 | Spring Boot + Redis | LLM chat API |
| `sentiment-analysis-service` | 8086 | Python + FastAPI | Kafka consumer, NLP |
| `market-data-processor` | 8087 | Python + FastAPI | Kafka consumer, DB writer |
| `chat-frontend` | 3000 | React | User interface |

## Data Flow

1. **Ingestion** – `news-ingestion-service` and `social-media-service` produce events to Kafka topics.
2. **Processing** – `sentiment-analysis-service` consumes raw topics, scores sentiment, publishes to `market.sentiment.scores`.
3. **Persistence** – `market-data-processor` consumes quotes and sentiment scores, writes to PostgreSQL.
4. **Query** – `market-data-service` exposes REST endpoints backed by PostgreSQL.
5. **Chat** – `chat-api-service` accepts user queries, optionally calls an LLM, caches history in Redis.
6. **Observability** – All Spring Boot services expose `/actuator/prometheus`; Prometheus scrapes + Grafana visualises.
