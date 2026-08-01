# Helm Chart

## Overview

The Helm chart at `helm/market-analysis/` packages the entire platform for reusable deployment across environments.

## Installing

```bash
helm upgrade --install market-analysis helm/market-analysis \
  --namespace market-analysis \
  --create-namespace \
  --values helm/market-analysis/values.yaml
```

## Key Values

```yaml
replicaCount:
  apiGateway: 2
  marketData: 2
  chatApi: 2

image:
  registry: registry.example.com
  tag: "1.0.0"

kafka:
  bootstrapServers: kafka:9092

postgres:
  host: postgres
  database: marketdb

credentials:
  dbUser: market
  dbPass: market     # override with --set or sealed-secrets
```

## Overriding Values

```bash
helm upgrade market-analysis helm/market-analysis \
  --set image.tag=2.0.0 \
  --set replicaCount.apiGateway=4
```

## Templated Resources

| Template | Description |
|----------|-------------|
| `configmap.yaml` | Shared environment variables |
| `secret.yaml` | DB credentials |
| `_helpers.tpl` | Shared template helpers |
