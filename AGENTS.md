# AGENTS.md

## Cursor Cloud specific instructions

### Product overview

**Codenames Online** is a single Ktor monolith: server-rendered FreeMarker UI, WebSockets for real-time lobby/game updates, H2 in-memory DB for local dev (`./gradlew run`), PostgreSQL when using Docker Compose.

### Services

| Service | When needed | Start command | URL |
|---------|-------------|---------------|-----|
| Ktor app | Always (dev & E2E) | `./gradlew run` | http://localhost:8080 |
| PostgreSQL | Optional (Docker/prod-like only) | `docker compose up` | localhost:5432 |

For day-to-day development, only `./gradlew run` is required (embedded H2, no Docker).

### JDK 24

The project targets **JDK 24** (`kotlin.jvmToolchain(24)`). The system JDK may be 21; Gradle auto-provisions Temurin 24 on first compile via toolchains (`~/.gradle/jdks/`). Do not change the system JDK unless Gradle toolchain resolution fails.

### Common commands

See [README.md](README.md) for full details. Quick reference:

| Task | Command |
|------|---------|
| Compile (CI lint job) | `./gradlew compileKotlin compileTestKotlin --no-daemon` |
| Tests | `./gradlew test --no-daemon` |
| Run dev server | `./gradlew run --no-daemon` |
| Health check | `curl http://localhost:8080/health` and `curl http://localhost:8080/health/ready` |

### Running the dev server

Use a **tmux** session so the server stays up across shell commands:

```bash
SESSION_NAME="ktor-dev-server"
tmux -f /exec-daemon/tmux.portal.conf has-session -t "=$SESSION_NAME" 2>/dev/null || \
  tmux -f /exec-daemon/tmux.portal.conf new-session -d -s "$SESSION_NAME" -c "/workspace" -- "${SHELL:-bash}" -l
tmux -f /exec-daemon/tmux.portal.conf send-keys -t "ktor-dev-server:0.0" './gradlew run --no-daemon' C-m
```

Wait for `GET /health` to return `{"status":"ok"}` (usually within ~10s after compile).

### Gotchas

- **No separate frontend build** — static JS/CSS is served from `src/main/resources/static/`.
- **CSRF on all POST forms** — browser flows work automatically; scripted HTTP tests must extract `_csrf` from the page or `data-csrf` on the lobby root.
- **Starting a game** requires 4 players with roles set (1 spymaster + 1 operative per team); see `TestFixtures.setupFourPlayerGame` in tests for the expected flow.
- **OpenAPI** is enabled in dev at `/openapi` (`app.exposeApiDocs: true` in `application.yaml`).
- **Docker Compose** needs `.env` from `.env.example` with `SESSION_SECRET`, `APP_PUBLIC_URL`, and DB credentials for a production-like stack.
