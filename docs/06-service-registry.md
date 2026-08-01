# Service Registry

The service registry uses **Netflix Eureka** (via Spring Cloud) for service discovery.

## Configuration

All microservices register themselves on startup:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://service-registry:8761/eureka/
```

## Dashboard

Accessible at `http://localhost:8761` when running locally.

```mermaid
graph TD
    SR[Eureka Server :8761]
    GW[api-gateway] --> SR
    NI[news-ingestion-service] --> SR
    MD[market-data-service] --> SR
    SM[social-media-service] --> SR
    CA[chat-api-service] --> SR
```

## Health Check

```bash
curl http://localhost:8761/actuator/health
```
