# CI/CD Pipeline

## 1. Purpose and Scope

This document explains the current GitLab CI/CD pipeline defined in `/home/runner/work/market-analysis/market-analysis/.gitlab-ci.yml`, including:

- How code is tested
- How containers are built and pushed
- How artifacts are promoted to Kubernetes environments
- How deployment validation should be performed

The focus is end-to-end traceability from commit to running containers.

---

## 2. Pipeline Stack Overview

## 2.1 Stages declared

The pipeline declares these stages:

1. `test`
2. `build`
3. `push`
4. `deploy`

Current behavior note:

- Image push is performed inside build jobs (there are no standalone `push` jobs).
- Deployment is performed with Helm in `deploy` jobs.

## 2.2 Execution model

- Jobs within the same stage run in parallel where possible.
- Later stages begin only when earlier stage jobs succeed.
- Production deploy is manual-gated.

## 2.3 Branch-to-environment mapping

- `develop` → auto deploy to `staging`
- `main` → manual deploy to `production`

---

## 3. Detailed Stage-by-Stage Flow

## Stage A: Test

Purpose: validate source quality before image creation.

### A1. Java microservice tests

Template: `.java-test`

- Runtime image: `eclipse-temurin:21`
- Tooling install: Maven via apt
- Command pattern: `mvn test -q`

Services tested:

- `service-registry`
- `api-gateway`
- `news-ingestion-service`
- `market-data-service`
- `social-media-service`
- `chat-api-service`

### A2. Python service tests

- Runtime image: `python:3.11-slim`
- Dependency install via `pip install -r requirements.txt -q`
- Test command: `python -m pytest tests/ -q || true`

Services:

- `sentiment-analysis-service`
- `market-data-processor`

Important quality note:

- `|| true` allows pipeline continuation even if pytest fails, so these tests are currently non-blocking.

### A3. Frontend tests

- Runtime image: `node:20-alpine`
- Command sequence:
  - `npm ci --quiet`
  - `npm test -- --watchAll=false`

Relationship to container pipeline:

- Only code that clears test stage should proceed to container builds, reducing the chance of publishing broken images.

---

## Stage B: Build (+ Registry Push)

Purpose: create deployable container images from each service and publish them.

### B1. Java image build template (`.java-build`)

Environment:

- Base image: `eclipse-temurin:21`
- Docker-in-Docker service: `docker:dind`
- Docker daemon via `DOCKER_HOST=tcp://docker:2375`

Build and publish steps:

1. Authenticate: `docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY`
2. Package app: `mvn package -DskipTests -q`
3. Build image with SHA tag
4. Push SHA tag
5. Tag same image as `latest`
6. Push `latest`

Services built:

- `service-registry`
- `api-gateway`
- `news-ingestion-service`
- `market-data-service`
- `social-media-service`
- `chat-api-service`

### B2. Python image build template (`.python-build`)

Environment:

- Base image: `docker:24`
- Docker-in-Docker service enabled

Flow:

1. Registry login
2. `docker build` from service directory
3. Push SHA tag
4. Tag as `latest`
5. Push `latest`

Services built:

- `sentiment-analysis-service`
- `market-data-processor`

### B3. Frontend image build

Dedicated `build:frontend` job performs equivalent Docker build/push logic for `chat-frontend`.

### B4. Tagging strategy and deployment safety

Each service publishes:

- Immutable tag: `$CI_COMMIT_SHORT_SHA` (best for traceability)
- Mutable tag: `latest` (easy consumption, lower auditability)

Relation to deployment:

- Deployment tooling can pick the SHA tag for deterministic rollouts.
- If workloads consume `latest`, explicit restart/rollout controls become critical to avoid drift.

---

## Stage C: Deploy

Purpose: release validated images to Kubernetes environments.

### C1. Staging deployment

Job: `deploy:staging`

- Runs automatically on `develop`
- Uses `alpine/helm:3.15.2`
- Command:
  - `helm upgrade --install market-analysis helm/market-analysis`
  - `--namespace market-analysis --create-namespace`
  - `--set image.tag=$CI_COMMIT_SHORT_SHA`
  - `--set image.registry=$REGISTRY`
  - `--wait`

### C2. Production deployment

Job: `deploy:production`

- Restricted to `main`
- `when: manual` approval gate
- Same Helm command pattern as staging

### C3. Deployment validation behavior

- `--wait` blocks until Helm marks release resources ready or times out.
- This gives immediate signal for basic rollout readiness in CI.

---

## 4. CI Variables and Secret Handling

Pipeline depends on:

- `CI_REGISTRY`
- `CI_REGISTRY_USER`
- `CI_REGISTRY_PASSWORD`
- `CI_REGISTRY_IMAGE`

Security expectations:

- Store credentials only in protected/masked CI variables.
- Never hardcode secrets in repository files.
- Keep runtime secrets (`DB_PASS`, `JWT_SECRET`, etc.) in Kubernetes Secrets or secret manager integrations.

---

## 5. End-to-End Traceability: Commit to Production

For a commit on `develop`:

1. Commit triggers pipeline.
2. Test jobs validate Java/Python/frontend components.
3. Build jobs package apps and produce Docker images.
4. Images are pushed with both SHA and `latest` tags.
5. Helm deploy runs using `image.tag=$CI_COMMIT_SHORT_SHA`.
6. Kubernetes pulls target images and rolls workloads.
7. Deployment waits for readiness (`--wait`).
8. Post-deploy checks confirm service health and data flow.

For a commit on `main`, production step repeats after manual approval.

---

## 6. Post-Deploy Testing and Validation Guide

Use these checks after deploy jobs complete.

## 6.1 Deployment-level checks

```bash
kubectl get deploy -n market-analysis
kubectl rollout status deployment/api-gateway -n market-analysis
kubectl rollout status deployment/market-data -n market-analysis
```

## 6.2 Pod and event checks

```bash
kubectl get pods -n market-analysis
kubectl get events -n market-analysis --sort-by=.lastTimestamp
```

## 6.3 Functional smoke tests

- Gateway health endpoint returns success.
- Frontend loads and can call chat/market endpoints.
- Ingestion endpoints trigger downstream processing.

Example gateway health check:

```bash
kubectl port-forward svc/api-gateway 8080:80 -n market-analysis
curl -i http://localhost:8080/actuator/health
```

## 6.4 Data-path validation

- Kafka consumers receive new events.
- Processor services log successful handling.
- PostgreSQL tables receive new rows for processed data.

---

## 7. Failure Scenarios and Fast Recovery

## 7.1 Test stage failures

- Java: inspect Maven test logs.
- Python: remove `|| true` if strict blocking behavior is required.
- Frontend: verify test environment assumptions in CI.

## 7.2 Build/push failures

Typical causes:

- Registry authentication errors
- Docker-in-Docker connectivity issues
- Dockerfile build breaks

Validate:

- CI variable values
- `docker login` success
- image naming consistency

## 7.3 Deploy failures

Typical causes:

- Helm template/config mismatch
- cluster permission issues
- workload readiness timeout

Validate:

```bash
helm list -n market-analysis
kubectl describe pods -n market-analysis
kubectl logs <pod-name> -n market-analysis --tail=200
```

Rollback option:

```bash
helm rollback market-analysis <revision> -n market-analysis
```

---

## 8. Practical Improvement Backlog

1. Add explicit `push` stage jobs or remove unused stage declaration.
2. Make Python tests blocking (remove `|| true`) once suites are stable.
3. Add security/quality gates (SAST, dependency scanning, lint).
4. Add immutable-image enforcement in Kubernetes manifests.
5. Add automated post-deploy smoke tests as a pipeline stage.
6. Expand Helm chart templates to fully own Deployments/Services.
