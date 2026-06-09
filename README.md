# Codenames Online

Multiplayer word-deduction game in the browser.

| | |
|---|---|
| **Backend** | Spring Boot 3 · Kotlin · REST · WebSockets · JPA · Flyway |
| **Frontend** | Vue 3 · TypeScript · Vite · vite-ssg |
| **Database** | PostgreSQL (prod) · H2 (local) |
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
cp .env.example .env   # set SESSION_SECRET, APP_PUBLIC_URL, DB credentials
docker compose up --build
# docker compose -f docker-compose.prod.yml up -d
```

Serves HTTP on port 8080. Terminate TLS at the load balancer.  
Config profile: `application-prod.yaml` (`SPRING_PROFILES_ACTIVE=prod`).

## Environment

| Variable | Description |
|----------|-------------|
| `DATABASE_*` | JDBC URL, user, password, driver |
| `SESSION_SECRET` | Session secret (32+ chars) |
| `APP_PUBLIC_URL` | Public site URL |
| `APP_ENV` | `dev` \| `prod` |
| `APP_SECURE_COOKIES` | `true` when behind HTTPS terminator |
| `APP_EXPOSE_API_DOCS` | Swagger UI at `/swagger-ui` (default off) |
| `HTTP_PORT` | Host port → container 8080 |
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

- State stored in PostgreSQL via JPA; Flyway migrations on startup.
- Real-time fan-out over WebSockets — **single-process only**; add Redis pub/sub before horizontal scaling.
- Landing page prerendered with vite-ssg for SEO; SPA served from `static/`.

## API

| Endpoint | Description |
|----------|-------------|
| `GET /health` | Liveness |
| `GET /health/ready` | DB readiness |
| `GET /swagger-ui` | API docs (when enabled) |
| `GET,POST /api/*` | JSON REST — rooms, session, locale, i18n |
| `GET /ws/rooms/{code}` | WebSocket game state |
