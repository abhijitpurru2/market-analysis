# market-analysis

Enterprise-style demo platform for financial news, market data, and social media analysis using Kubernetes + Spring Boot + Kafka + Python + an LLM chat stack.

## Deployment and Running

This repository now uses Kafka in **KRaft mode** (no ZooKeeper dependency) in Docker Compose and Kubernetes manifests.

### 1) Local (no containers for app services)

#### Prerequisites

Based on repository files (`*/pom.xml`, Python Dockerfiles/requirements, `chat-frontend/package.json`, `chat-frontend/Dockerfile`):

- Java 21
- Maven (version not pinned in-repo; use a recent Maven release)
- Python 3.11
- Node.js 20 + npm
- Docker + Docker Compose (for infra dependencies if not installed natively)

#### Start required infrastructure

For local app-service development, Kafka/PostgreSQL/Redis must still be available. You can run infra-only containers:

```bash
docker compose up -d kafka postgres redis
```

#### Run Java services (`mvn spring-boot:run`)

```bash
cd service-registry && mvn spring-boot:run
cd ../api-gateway && mvn spring-boot:run
cd ../news-ingestion-service && mvn spring-boot:run
cd ../market-data-service && mvn spring-boot:run
cd ../social-media-service && mvn spring-boot:run
cd ../chat-api-service && mvn spring-boot:run
```

Required/used environment variables from `application.yml` files:

- `api-gateway`:
  - `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`
- `news-ingestion-service`:
  - `SPRING_KAFKA_BOOTSTRAP_SERVERS`
  - `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`
- `market-data-service`:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `SPRING_KAFKA_BOOTSTRAP_SERVERS`
  - `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`
- `social-media-service`:
  - `SPRING_KAFKA_BOOTSTRAP_SERVERS`
  - `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`
- `chat-api-service`:
  - `SPRING_REDIS_HOST`
  - `SPRING_REDIS_PORT`
  - `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`
  - `LLM_ENDPOINT`
  - `LLM_MODEL`
- `service-registry`:
  - No required external env vars in `application.yml` defaults

#### Run Python services (venv + uvicorn)

```bash
cd sentiment-analysis-service
python3.11 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8086

cd ../market-data-processor
python3.11 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8087
```

Required/used Python environment variables from `main.py`:

- `sentiment-analysis-service`:
  - `KAFKA_BOOTSTRAP_SERVERS`
- `market-data-processor`:
  - `KAFKA_BOOTSTRAP_SERVERS`
  - `DB_HOST`
  - `DB_PORT`
  - `DB_NAME`
  - `DB_USER`
  - `DB_PASS`

#### Run frontend

```bash
cd chat-frontend
npm install
npm start
```

Frontend env/config used by source:

- `REACT_APP_API_BASE` (default `/api/chat` from `src/App.js`)
- `chat-frontend/package.json` `proxy` is set to `http://api-gateway:8080`

### 2) Docker (Docker Compose)

From repository root:

```bash
docker compose up --build
```

Infra only:

```bash
docker compose up -d kafka postgres redis
```

Port mappings from `docker-compose.yml`:

- `kafka`: `9092:9092`
- `postgres`: `5432:5432`
- `redis`: `6379:6379`
- `prometheus`: `9090:9090`
- `grafana`: `3001:3000`
- `service-registry`: `8761:8761`
- `api-gateway`: `8080:8080`
- `chat-api`: `8085:8085`
- `chat-frontend`: `3000:80`

Useful commands:

```bash
docker compose logs -f
docker compose logs -f kafka
docker compose down
```

### 3) Kubernetes (raw manifests + Helm)

#### Raw manifests

Apply in this order (matches `docs/15-kubernetes-deployment.md`):

```bash
kubectl apply -f k8s/00-namespace.yml
kubectl apply -f k8s/01-infrastructure.yml
kubectl apply -f k8s/02-services.yml
```

#### Helm chart

Chart location:

- `helm/market-analysis/`

Install/upgrade:

```bash
helm upgrade --install market-analysis helm/market-analysis --namespace market-analysis --create-namespace --values helm/market-analysis/values.yaml
```

Override values example:

```bash
helm upgrade --install market-analysis helm/market-analysis \
  --namespace market-analysis \
  --create-namespace \
  --values helm/market-analysis/values.yaml \
  --set image.tag=latest \
  --set replicaCount.apiGateway=2
```

Chart structure (current repository state):

- `helm/market-analysis/Chart.yaml`
- `helm/market-analysis/values.yaml`
- `helm/market-analysis/templates/configmap.yaml`
- `helm/market-analysis/templates/secret.yaml`
- `helm/market-analysis/templates/_helpers.tpl`
- TODO: no Deployment/Service templates are currently present under `templates/`
