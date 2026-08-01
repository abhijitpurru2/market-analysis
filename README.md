# market-analysis

Enterprise-style demo platform for financial news, market data, and social media analysis using Kubernetes + Spring Boot + Kafka + Python + an LLM chat stack.

## Deployment and Running

### Architecture at a glance

- **Java / Spring Boot services (Java 17)**:
  - `service-registry` (Eureka, port `8761`)
  - `api-gateway` (port `8080`)
  - `news-ingestion-service` (port `8081`)
  - `market-data-service` (port `8082`)
  - `social-media-service` (port `8083`)
  - `chat-api-service` (port `8085`)
- **Python / FastAPI services (Python 3.11)**:
  - `sentiment-analysis-service` (port `8086`)
  - `market-data-processor` (port `8087`)
- **Frontend**:
  - `chat-frontend` (React; dev server `3000`, Docker/Nginx `80` mapped to host `3000`)
- **Infra**:
  - Kafka (`9092`), Zookeeper (`2181`), PostgreSQL (`5432`), Redis (`6379`), Prometheus (`9090`), Grafana (`3001`)

### Prerequisites

Based on `pom.xml`, `requirements.txt`, `package.json`, Dockerfiles, and CI:

- **Java**: 17
- **Maven**: Maven 3.9+ recommended
- **Python**: 3.11
- **Node.js**: 20.x (CI uses `node:20-alpine`)
- **npm**: comes with Node 20
- **Docker** + **Docker Compose** (Compose file version `3.9`)

### Build each component locally

From repository root (`/home/runner/work/market-analysis/market-analysis`):

#### Java services

```bash
cd service-registry && mvn clean package
cd ../api-gateway && mvn clean package
cd ../news-ingestion-service && mvn clean package
cd ../market-data-service && mvn clean package
cd ../social-media-service && mvn clean package
cd ../chat-api-service && mvn clean package
```

#### Python services

```bash
cd sentiment-analysis-service
python3.11 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

cd ../market-data-processor
python3.11 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

#### Frontend

```bash
cd chat-frontend
npm install
npm run build
```

### Run locally (development mode)

#### 1) Start required infrastructure

Use Docker Compose for Kafka/Postgres/Redis/etc:

```bash
docker compose up -d zookeeper kafka postgres redis service-registry
```

You can also start everything with:

```bash
docker compose up --build
```

#### 2) Run Java services

Each service uses Spring Boot defaults from `application.yml` and environment-variable overrides:

```bash
cd service-registry && mvn spring-boot:run
cd ../api-gateway && mvn spring-boot:run
cd ../news-ingestion-service && mvn spring-boot:run
cd ../market-data-service && mvn spring-boot:run
cd ../social-media-service && mvn spring-boot:run
cd ../chat-api-service && mvn spring-boot:run
```

Important environment variables used by Java services:

- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` (default `http://localhost:8761/eureka/`)
- `SPRING_KAFKA_BOOTSTRAP_SERVERS` (default `localhost:9092`)
- `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` (market-data-service)
- `SPRING_REDIS_HOST` / `SPRING_REDIS_PORT` (chat-api-service)
- `LLM_ENDPOINT` / `LLM_MODEL` (chat-api-service)

#### 3) Run Python services

```bash
cd sentiment-analysis-service
uvicorn main:app --host 0.0.0.0 --port 8086

cd ../market-data-processor
uvicorn main:app --host 0.0.0.0 --port 8087
```

Environment variables used by Python services:

- `KAFKA_BOOTSTRAP_SERVERS` (default `localhost:9092`)
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS` (market-data-processor)

#### 4) Run frontend

```bash
cd chat-frontend
npm start
```

Frontend config:
- `REACT_APP_API_BASE` (optional, default `/api/chat`)
- `package.json` proxy points to `http://api-gateway:8080` (mainly for container/network setups)

### Run with Docker Compose

From repo root:

```bash
docker compose up --build
```

To stop:

```bash
docker compose down
```

### Ports and service interaction

- Frontend (`3000`) calls API paths under `/api/*`.
- Nginx in frontend container proxies `/api/` to `api-gateway:8080`.
- API Gateway routes:
  - `/api/news/**` -> `news-ingestion-service`
  - `/api/market/**` -> `market-data-service`
  - `/api/social/**` -> `social-media-service`
  - `/api/chat/**` -> `chat-api-service`
- News, market, and social services publish to Kafka topics.
- Python services consume Kafka topics and write processed data (market-data-processor -> PostgreSQL).
- Chat API stores chat history in Redis and can call an LLM endpoint.

### Troubleshooting notes

- **Service discovery**: if Java services fail to route, verify `service-registry` is up on `8761` and that `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` is reachable.
- **Kafka connection errors**: verify broker at `localhost:9092` (or `kafka:29092` in Docker network).
- **Database errors**: verify PostgreSQL credentials/host match `SPRING_DATASOURCE_*` and `DB_*` variables.
- **LLM responses fallback**: chat service falls back to demo responses if `LLM_ENDPOINT` is unavailable.
- **Tests status**: repository currently has very limited test coverage and some CI test steps are permissive (`|| true` in Python test jobs).

### TODO / unknowns to confirm

- No `.env.example` file is present in root or service directories.
- Production secret management strategy is not documented in this repository.
