# Codenames Online

Multiplayer word-deduction game in the browser.

| | |
|---|---|
| **Backend** | Spring Boot 3 · Kotlin · REST · WebSockets · JPA · Flyway |
| **Frontend** | Vue 3 · TypeScript · Vite · vite-ssg |
| **Database** | PostgreSQL (prod) · H2 (local) |
| **Cache / Pub-Sub** | Redis 7 — shared sessions · WS broadcast · rate-limit |
| **Edge proxy** | Caddy 2 — auto-TLS (Let's Encrypt) · LB · WS upgrade |
| **Analytics** | Self-hosted Umami (optional) |

## Requirements

- JDK 24
- Node.js 26 ([`.nvmrc`](.nvmrc)) · pnpm 10
- Docker (optional, production stack)

## Local dev

```bash
# Backend (H2 in-memory, no Postgres needed)
./gradlew bootRun       # http://localhost:8080
./gradlew test

# Frontend (proxies /api, /ws → :8080)
cd frontend
corepack enable && corepack prepare pnpm@10.11.0 --activate
pnpm install
pnpm dev                # http://localhost:5173
```

Other frontend commands: `pnpm test` (Vitest), `pnpm run typecheck` (vue-tsc), `pnpm run lint`, `pnpm run format`.

Build frontend into `src/main/resources/static/` for Spring-only serving: `pnpm build && ./gradlew bootRun`.

## Docker

```bash
cp .env.example .env   # set SESSION_SECRET, APP_DOMAIN, REDIS_PASSWORD, DB credentials
docker compose up --build         # local dev (single replica, no Caddy)
docker compose -f docker-compose.prod.yml up -d --scale app=3   # prod (3 replicas)
```

Production stack (`docker-compose.prod.yml`):
- **Caddy** terminates TLS automatically via Let's Encrypt (ports 80 + 443 must be open; `APP_DOMAIN` DNS must point to the host).
- **Redis** is used for shared HTTP sessions, WebSocket pub/sub broadcasts, and distributed rate-limiting.
- **Replicas** are scaled with `--scale app=N`; Caddy discovers them via Docker DNS (`dynamic a app 8080`) and balances round-robin.

Config profile: `application-prod.yaml` (`SPRING_PROFILES_ACTIVE=prod`).  
WebSocket connections (`/ws/rooms/*`) are proxied transparently by Caddy — no extra config needed.

## Environment

| Variable | Description |
|----------|-------------|
| `DATABASE_*` | JDBC URL, user, password, driver |
| `SESSION_SECRET` | Session secret (32+ chars) |
| `APP_DOMAIN` | Public domain — used by Caddy for auto-TLS and by the app |
| `APP_PUBLIC_URL` | Canonical public URL (`https://{APP_DOMAIN}`) |
| `APP_ENV` | `dev` \| `prod` |
| `APP_SECURE_COOKIES` | `true` in prod (cookies sent only over HTTPS) |
| `APP_EXPOSE_API_DOCS` | Swagger UI at `/swagger-ui` (default off) |
| `REDIS_PASSWORD` | Redis auth password (optional; omit for no-auth) |
| `ACME_EMAIL` | Email for Let's Encrypt expiry notifications (optional) |
| `VITE_PUBLIC_URL` | Canonical URL baked into frontend SEO at build time |
| `VITE_UMAMI_*` | Umami script URL + website ID (build-time, optional) |
| `UMAMI_*` | Self-hosted Umami — DB, secret, port (`docker-compose.prod.yml`) |

Full template: [`.env.example`](.env.example).  
CI secret `DEPLOY_DOTENV` mirrors `.env.example`; `IMAGE_TAG` is set on tag push.

## Architecture

```
com.lalkalol/
├── common/model/   shared enums (Team, Role, Language, RoomStatus)
├── game/           board, turns, game state, DTOs, WS messages
├── room/           lobby, players, lifecycle, REST + WebSocket handlers
├── words/          dictionary seed + lookup
├── db/             JPA entities and repositories
├── i18n/           messages, locale API
├── web/            session, SPA routing, security, error DTOs
└── config/         Spring config, health endpoints
```

- State stored in PostgreSQL via JPA; Flyway migrations on startup (DB-level lock, safe with multiple replicas).
- Real-time fan-out over WebSockets via Redis pub/sub — each replica broadcasts to its local connections on receipt.
- HTTP sessions shared via Spring Session Redis; no sticky sessions required.
- Landing page prerendered with vite-ssg for SEO; SPA served from `static/`.

## API

| Endpoint | Description |
|----------|-------------|
| `GET /health` | Liveness |
| `GET /health/ready` | DB readiness |
| `GET /swagger-ui` | API docs (when enabled) |
| `GET,POST /api/*` | JSON REST — rooms, session, locale, i18n |
| `GET /ws/rooms/{code}` | WebSocket game state |
