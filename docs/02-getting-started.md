# Getting Started

## Prerequisites

| Tool | Minimum Version |
|------|----------------|
| Docker | 24.x |
| Docker Compose | 2.x |
| Java | 17 |
| Maven | 3.9 |
| Node.js | 20 |
| Python | 3.11 |

## Quick Start (Docker Compose)

```bash
# Clone the repository
git clone https://github.com/your-org/market-analysis.git
cd market-analysis

# Build all Java services
for svc in service-registry api-gateway news-ingestion-service \
           market-data-service social-media-service chat-api-service; do
  (cd $svc && mvn package -DskipTests -q)
done

# Start the full stack
docker compose up -d

# Verify services are healthy
docker compose ps
```

## Accessing Services

| UI | URL |
|----|-----|
| Chat Frontend | http://localhost:3000 |
| API Gateway | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 (admin/admin) |

## Running the Demo Generator

```bash
cd demo-data-generators
pip install -r requirements.txt
python generate_market_data.py
```

## Stopping

```bash
docker compose down -v
```
