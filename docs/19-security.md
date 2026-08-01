# Security

## Authentication & Authorisation

The demo platform does not enforce authentication by default. For production hardening:

### API Gateway – JWT Validation

Add Spring Security OAuth2 Resource Server to `api-gateway`:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.example.com
```

### mTLS Between Services

For service-to-service calls inside the cluster, enable mTLS via a service mesh (Istio, Linkerd).

## Secret Management

### Development

Secrets are stored in Kubernetes `Secret` objects (base64-encoded). Use SealedSecrets or HashiCorp Vault for production.

### Production Recommendations

1. **HashiCorp Vault** – Dynamic secrets and lease management.
2. **AWS Secrets Manager / GCP Secret Manager** – Cloud-native options.
3. **Kubernetes SealedSecrets** – GitOps-friendly encrypted secrets.

## Kafka Security

Enable SASL/SCRAM for Kafka in production:

```properties
security.protocol=SASL_SSL
sasl.mechanism=SCRAM-SHA-256
```

## Data at Rest

- PostgreSQL: Enable TDE (Transparent Data Encryption) at the storage layer.
- Redis: Enable AOF + RDB persistence with file-level encryption.

## OWASP Top 10 Mitigations

| Risk | Mitigation |
|------|-----------|
| Injection | JPA parameterised queries, psycopg2 placeholders |
| Broken Auth | JWT + OAuth2 Resource Server |
| Sensitive Data Exposure | HTTPS everywhere, secrets in Vault |
| SSRF | Restrict outbound HTTP to allowlist |
| Logging & Monitoring | Prometheus + Grafana + structured logs |
