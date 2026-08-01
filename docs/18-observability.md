# Observability

## Metrics

All Spring Boot services expose Prometheus metrics at `/actuator/prometheus`.

### Scrape Configuration

Prometheus is pre-configured in `infra/prometheus/prometheus.yml` to scrape all services.

```bash
# Access Prometheus
open http://localhost:9090
```

### Key Metrics

| Metric | Description |
|--------|-------------|
| `http_server_requests_seconds` | HTTP latency per endpoint |
| `kafka_producer_record_send_total` | Messages published to Kafka |
| `jvm_memory_used_bytes` | JVM heap usage |
| `process_cpu_usage` | CPU utilisation |

## Grafana Dashboards

Grafana is provisioned at `http://localhost:3001` (admin/admin).

The datasource is pre-configured to point at Prometheus.

To import a community dashboard (e.g. Spring Boot):
1. Open Grafana → + → Import
2. Enter dashboard ID `12685`
3. Select Prometheus datasource

## Logging

All services use structured logging via SLF4J + Logback (Java) and Python `logging` module.

```mermaid
flowchart LR
    SVC[Microservice] --> LOG[stdout]
    LOG --> COLLECT[Log aggregator]
    COLLECT --> ES[(Elasticsearch)]
    ES --> KIBANA[Kibana]
```

For production, pipe stdout to Fluentd or Loki.

## Distributed Tracing

Add Spring Cloud Sleuth + Zipkin for distributed traces:

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```
