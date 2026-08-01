# Contributing Guide

## Development Setup

1. **Fork** the repository and create a feature branch:
   ```bash
   git checkout -b feature/my-feature
   ```

2. **Install prerequisites**: JDK 21, Maven 3.9, Node.js 20, Python 3.11, Docker.

3. **Start infrastructure only**:
   ```bash
   docker compose up -d zookeeper kafka postgres redis
   ```

4. **Run services locally** for fast iteration:
   ```bash
   cd market-data-service && mvn spring-boot:run
   ```

## Code Style

### Java
- Google Java Style (enforced via Checkstyle)
- Lombok for boilerplate reduction
- Records or `@Data` for DTOs

### Python
- PEP 8 (enforced via ruff)
- Type hints on all public functions
- Pydantic models for request/response schemas

### React
- Functional components with hooks
- Prop types or TypeScript interfaces
- CSS modules or scoped CSS

## Testing

### Java
```bash
mvn test
```

### Python
```bash
pytest tests/ -v
```

### React
```bash
npm test
```

## Pull Request Process

1. Ensure all tests pass locally.
2. Update documentation if you change an API or architecture decision.
3. Add a brief description of changes in the PR body.
4. Request review from at least one team member.
5. Squash-merge after approval.

## Commit Message Convention

```
type(scope): short description

[optional body]
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

Examples:
- `feat(chat-api): add WebSocket support`
- `fix(market-data): correct changePercent calculation`
- `docs(kafka): update topic schema for sentiment scores`
