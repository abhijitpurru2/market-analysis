# Kubernetes Deployment

## Overview

The platform ships with production-ready Kubernetes manifests in the `k8s/` directory.

## Files

| File | Description |
|------|-------------|
| `00-namespace.yml` | Namespace, ConfigMap, Secrets |
| `01-infrastructure.yml` | Zookeeper, Kafka, PostgreSQL |
| `02-services.yml` | All application microservices |

## Deploying

```bash
# Apply all manifests in order
kubectl apply -f k8s/00-namespace.yml
kubectl apply -f k8s/01-infrastructure.yml
kubectl apply -f k8s/02-services.yml
```

## Namespace

All resources are deployed to the `market-analysis` namespace.

## Architecture Diagram

```mermaid
graph TB
    subgraph K8s Cluster
        subgraph market-analysis namespace
            GW[api-gateway x2]
            NI[news-ingestion x1]
            MD[market-data x2]
            SM[social-media x1]
            CA[chat-api x2]
            FE[chat-frontend x2]
            SR[service-registry x1]
            SA[sentiment-analysis x1]
            MDP[market-data-processor x1]
            subgraph Infrastructure
                K[kafka]
                PG[postgres]
                RD[redis]
                ZK[zookeeper]
            end
        end
    end
    Internet --> GW
    Internet --> FE
```

## Scaling

```bash
kubectl scale deployment market-data --replicas=4 -n market-analysis
```
