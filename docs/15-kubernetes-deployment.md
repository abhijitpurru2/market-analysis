# Kubernetes Deployment

## 1. Purpose

This guide documents the **current Kubernetes deployment model** for the `market-analysis` platform and explains each step from image creation to workload rollout, health validation, and post-deployment checks.

It is aligned with the repository state:

- Raw manifests in `/home/runner/work/market-analysis/market-analysis/k8s`
- CI pipeline in `/home/runner/work/market-analysis/market-analysis/.gitlab-ci.yml`
- Helm chart skeleton in `/home/runner/work/market-analysis/market-analysis/helm/market-analysis`

---

## 2. Deployment Assets and Their Roles

## 2.1 Manifest ordering

Apply files in this order:

1. `k8s/00-namespace.yml`
2. `k8s/01-infrastructure.yml`
3. `k8s/02-services.yml`

Why order matters:

- `00-namespace.yml` creates namespace + shared config + secrets consumed later by Deployments.
- `01-infrastructure.yml` starts stateful dependencies (`kafka`, `postgres`) required by service startup.
- `02-services.yml` deploys the application services that depend on config, secrets, and infra DNS names.

## 2.2 What each file contains

| File | Resource Types | Main Responsibility |
|---|---|---|
| `00-namespace.yml` | Namespace, ConfigMap, Secret | Defines runtime namespace and common environment data |
| `01-infrastructure.yml` | Deployments + Services | Runs Kafka (KRaft single node) and PostgreSQL |
| `02-services.yml` | Deployments + Services | Runs API gateway, ingestion, data, social, chat, and frontend services |

---

## 3. Pre-Deployment Requirements

Before deploying, verify:

- Kubernetes cluster reachable: `kubectl cluster-info`
- Correct context selected: `kubectl config current-context`
- Namespace permissions available
- Container registry access (for non-`market/*` local images)
- Required secrets values prepared:
  - `DB_USER`
  - `DB_PASS`
  - `JWT_SECRET`
  - `REDIS_PASSWORD`

Important current-state note:

- `k8s/02-services.yml` references `redis` (`SPRING_REDIS_HOST=redis`) but no Redis Deployment/Service exists in `k8s/` files. Add Redis separately in your environment before full runtime validation.

---

## 4. End-to-End Flow: Container Creation to Running Pods

## 4.1 Build and tag images

Pipeline build jobs create service images and push:

- Immutable tag: `$CI_COMMIT_SHORT_SHA`
- Rolling tag: `latest`

Relationship to Kubernetes:

- Kubernetes Deployments in `k8s/02-services.yml` currently point to static names such as `market/api-gateway:latest`.
- This means rollout behavior follows `latest` updates unless manifests are changed to SHA-pinned tags.

## 4.2 Push images to registry

CI build jobs authenticate to `$CI_REGISTRY` and push all images.

Why this matters:

- Kubernetes nodes pull the published images during pod scheduling.
- If pull access or tags are wrong, pods remain in `ImagePullBackOff`.

## 4.3 Apply Kubernetes resources

```bash
kubectl apply -f /home/runner/work/market-analysis/market-analysis/k8s/00-namespace.yml
kubectl apply -f /home/runner/work/market-analysis/market-analysis/k8s/01-infrastructure.yml
kubectl apply -f /home/runner/work/market-analysis/market-analysis/k8s/02-services.yml
```

## 4.4 Kubernetes schedules and starts containers

Per Deployment, Kubernetes:

1. Creates ReplicaSet
2. Creates Pods
3. Pulls image
4. Injects env vars from ConfigMap/Secret
5. Starts container process
6. Exposes pods through Services

## 4.5 Readiness and traffic routing

- `api-gateway` includes a readiness probe (`/actuator/health` on port `8080`).
- Pod traffic begins only after readiness is successful.
- Services route to matching pod labels once endpoints are ready.

---

## 5. Detailed Deployment Procedure

## Step 1: Validate manifests before apply

```bash
kubectl apply --dry-run=client -f /home/runner/work/market-analysis/market-analysis/k8s/00-namespace.yml
kubectl apply --dry-run=client -f /home/runner/work/market-analysis/market-analysis/k8s/01-infrastructure.yml
kubectl apply --dry-run=client -f /home/runner/work/market-analysis/market-analysis/k8s/02-services.yml
```

## Step 2: Create namespace, config, and secret envelope

Apply `00-namespace.yml`, then confirm:

```bash
kubectl get ns market-analysis
kubectl get configmap market-config -n market-analysis
kubectl get secret market-secrets -n market-analysis
```

## Step 3: Ensure secret values are populated

If values are empty, patch them before app rollout:

```bash
kubectl create secret generic market-secrets \
  -n market-analysis \
  --from-literal=DB_USER='<db-user>' \
  --from-literal=DB_PASS='<db-pass>' \
  --from-literal=JWT_SECRET='<jwt-secret>' \
  --from-literal=REDIS_PASSWORD='<redis-password>' \
  --dry-run=client -o yaml | kubectl apply -f -
```

## Step 4: Deploy infrastructure services

Apply `01-infrastructure.yml`, then validate:

```bash
kubectl get deploy,po,svc -n market-analysis | grep -E 'kafka|postgres'
kubectl rollout status deployment/kafka -n market-analysis
kubectl rollout status deployment/postgres -n market-analysis
```

## Step 5: Deploy application services

Apply `02-services.yml`, then validate each deployment:

```bash
kubectl get deployments -n market-analysis
kubectl rollout status deployment/api-gateway -n market-analysis
kubectl rollout status deployment/news-ingestion -n market-analysis
kubectl rollout status deployment/market-data -n market-analysis
kubectl rollout status deployment/social-media -n market-analysis
kubectl rollout status deployment/chat-api -n market-analysis
kubectl rollout status deployment/chat-frontend -n market-analysis
```

## Step 6: Verify service discovery and endpoints

```bash
kubectl get svc -n market-analysis
kubectl get endpoints -n market-analysis
```

## Step 7: Runtime smoke tests

Use port-forward for quick validation:

```bash
kubectl port-forward svc/api-gateway 8080:80 -n market-analysis
curl -i http://localhost:8080/actuator/health
```

Frontend check:

```bash
kubectl port-forward svc/chat-frontend 3000:80 -n market-analysis
# open http://localhost:3000
```

---

## 6. Validation Checklist (Post Deployment)

Use this checklist for environment sign-off.

## 6.1 Kubernetes resource health

- All Deployments show `AVAILABLE` replicas matching desired count.
- No pod in `CrashLoopBackOff`, `ImagePullBackOff`, or `ErrImagePull`.
- Services have endpoints.

Commands:

```bash
kubectl get deploy -n market-analysis
kubectl get pods -n market-analysis
kubectl get events -n market-analysis --sort-by=.lastTimestamp
```

## 6.2 Application health

- `api-gateway` readiness endpoint returns `UP`.
- Backend service logs show successful connection to Kafka/Postgres.
- Frontend loads and can call gateway routes.

Commands:

```bash
kubectl logs deployment/api-gateway -n market-analysis --tail=200
kubectl logs deployment/market-data -n market-analysis --tail=200
kubectl logs deployment/chat-api -n market-analysis --tail=200
```

## 6.3 Data flow verification

- Ingestion APIs can trigger events.
- Kafka topics receive traffic.
- Processor writes to PostgreSQL.

Suggested checks:

- Trigger ingestion endpoints via gateway.
- Verify consumer logs (`market-data-processor`, `sentiment-analysis-service` if deployed).
- Query PostgreSQL tables for newly inserted rows.

---

## 7. Rollout, Update, and Rollback Model

## 7.1 Updating service containers

Current model options:

1. Re-push `latest` and restart Deployments:
   ```bash
   kubectl rollout restart deployment/api-gateway -n market-analysis
   ```
2. Prefer immutable rollout using explicit image tag:
   ```bash
   kubectl set image deployment/api-gateway \
     api-gateway=<registry>/<path>/api-gateway:<sha> -n market-analysis
   ```

## 7.2 Monitoring rollout progress

```bash
kubectl rollout status deployment/api-gateway -n market-analysis
kubectl describe deployment api-gateway -n market-analysis
```

## 7.3 Rollback

```bash
kubectl rollout undo deployment/api-gateway -n market-analysis
kubectl rollout history deployment/api-gateway -n market-analysis
```

---

## 8. Failure Points and Quick Diagnosis

## 8.1 Image pull failures

Symptoms: `ImagePullBackOff`.

Checks:

- Image exists in registry with expected tag.
- Cluster can authenticate to registry.
- Image name matches Deployment spec.

## 8.2 Config/secret failures

Symptoms: startup exceptions for DB/JWT/Redis.

Checks:

```bash
kubectl get secret market-secrets -n market-analysis -o yaml
kubectl get configmap market-config -n market-analysis -o yaml
```

## 8.3 Dependency readiness issues

Symptoms: services start but fail to connect to Kafka/Postgres/Redis.

Checks:

- Dependency pod status
- Service DNS names
- Network policies/firewalls
- Startup order and retry behavior

---

## 9. Helm Relationship in Current Repository

Current Helm chart (`helm/market-analysis`) primarily defines:

- `configmap.yaml`
- `secret.yaml`
- helper templates

CI deploy jobs run `helm upgrade --install ... --set image.tag=... --set image.registry=...`.

Operational interpretation:

- Helm is present as deployment interface for chart-managed resources.
- Full workload deployment detail is still explicitly represented in `k8s/*.yml` manifests.
- Keep both in sync if you extend Helm templates to include Deployments/Services.

---

## 10. Recommended Hardening Next Steps

1. Add Redis manifests (or managed Redis integration) for Kubernetes parity.
2. Move all runtime workloads into Helm templates for single-source environment promotion.
3. Pin Deployments to immutable SHA tags by default.
4. Add liveness probes for all services and readiness probes for all API workloads.
5. Add persistent volumes for PostgreSQL in production (avoid `emptyDir`).
6. Add pod resource requests/limits to prevent noisy-neighbor failures.
