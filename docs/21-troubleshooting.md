# Troubleshooting

## Common Issues

### Services Not Registering with Eureka

**Symptom**: Services don't appear in Eureka dashboard at `http://localhost:8761`.

**Check**:
```bash
docker compose logs service-registry
docker compose logs api-gateway
```

**Fix**: Ensure `service-registry` is healthy before other services start. Add `depends_on` with health check condition.

---

### Kafka Connection Refused

**Symptom**: `LEADER_NOT_AVAILABLE` or connection refused errors.

**Check**:
```bash
docker compose logs kafka
docker compose exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```

**Fix**: Verify Kafka KRaft listener settings are internally consistent (`KAFKA_LISTENERS`, `KAFKA_ADVERTISED_LISTENERS`, `KAFKA_LISTENER_SECURITY_PROTOCOL_MAP`, `KAFKA_CONTROLLER_LISTENER_NAMES`, `KAFKA_CONTROLLER_QUORUM_VOTERS`) and check `KAFKA_ADVERTISED_LISTENERS` matches the hostname consumers use.

---

### PostgreSQL Auth Failure

**Symptom**: `FATAL: password authentication failed for user "market"`.

**Fix**: Ensure `SPRING_DATASOURCE_PASSWORD` / `DB_PASS` env vars match the `POSTGRES_PASSWORD` set on the PostgreSQL container.

---

### Redis Connection Error

**Symptom**: `Unable to connect to Redis`.

**Fix**: Verify `SPRING_REDIS_HOST` and `SPRING_REDIS_PORT` env vars point to the correct Redis instance.

---

### Chat API Returns Demo Response

**Symptom**: Chat answers are generic, not LLM-generated.

**Fix**: Set `LLM_ENDPOINT` to a running Ollama instance:
```bash
docker run -d -p 11434:11434 ollama/ollama
docker exec ollama ollama pull llama3
```

---

### React Frontend 502 Bad Gateway

**Symptom**: API calls from the frontend return 502.

**Fix**: Verify nginx.conf `proxy_pass` points to `api-gateway:8080` and that the api-gateway container is healthy.

---

## Useful Commands

```bash
# View all service logs
docker compose logs -f

# Restart a single service
docker compose restart market-data

# Check Kafka consumer lag
docker compose exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group market-data-processor-quotes

# Connect to PostgreSQL
docker compose exec postgres psql -U market marketdb
```
