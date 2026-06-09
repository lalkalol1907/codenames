# Codenames Online

Multiplayer word-deduction game in the browser (Codenames-style).

| | |
|---|---|
| **Backend** | Spring Boot 3 · Kotlin · REST · WebSockets |
| **Frontend** | Vue 3 · TypeScript · Vite · vite-ssg |
| **Database** | PostgreSQL (Docker/prod) · H2 (local `./gradlew bootRun`) |

## Requirements

- JDK **24**
- **Node.js 26.3.0** (see [`.nvmrc`](.nvmrc)) and **pnpm 10**
- Docker — optional (production-like stack)

## Local development

Backend:

```bash
./gradlew bootRun    # → http://localhost:8080
./gradlew test
```

Embedded H2 — no Postgres or Docker required.

Frontend (Vite dev server with API proxy):

```bash
nvm use
cd frontend
corepack enable && corepack prepare pnpm@10.11.0 --activate
pnpm install
pnpm run lint          # ESLint + Prettier
pnpm run format        # Prettier write
pnpm test              # Vitest
pnpm run typecheck     # vue-tsc
pnpm dev         # → http://localhost:5173 (proxies /api, /ws to :8080)
pnpm build       # → src/main/resources/static (vite-ssg, prerendered landing)
```

Production-like UI from Spring only: run `pnpm build` in `frontend/`, then `./gradlew bootRun`.

## Docker

```bash
cp .env.example .env
# Set SESSION_SECRET, APP_PUBLIC_URL, and DB credentials

docker compose up --build
# Production image deploy:
# docker compose -f docker-compose.prod.yml up -d
```

The app serves HTTP on port 8080. Terminate TLS at a load balancer in production.

Config profile: `application-prod.yaml` (`SPRING_PROFILES_ACTIVE=prod`).

## Environment

| Variable | Description |
|----------|-------------|
| `DATABASE_*` | JDBC URL, user, password, driver |
| `SESSION_SECRET` | Session secret (32+ random chars) |
| `APP_PUBLIC_URL` | Public site URL |
| `APP_ENV` | `dev` \| `prod` |
| `APP_SECURE_COOKIES` | `true` when users reach the site over HTTPS (e.g. via LB) |
| `APP_EXPOSE_API_DOCS` | Swagger UI at `/swagger-ui` (default off in prod) |
| `HTTP_PORT` | Host port mapped to container 8080 |
| `VITE_PUBLIC_URL` | Canonical URL baked into frontend SEO meta at build time |
| `VITE_UMAMI_*` | Umami script URL and website id (baked at frontend build; optional) |
| `UMAMI_*` | Self-hosted Umami in `docker-compose.prod.yml` (DB, secret, port) |

Full template: [`.env.example`](.env.example).

GitHub secret `DEPLOY_DOTENV` should mirror `.env.example` (without local-only vars); CI sets `IMAGE_TAG` on tag push.

## Analytics (Umami)

Self-hosted [Umami](https://umami.is/) tracks page views and custom events. Without `VITE_UMAMI_*` the frontend skips analytics (local dev).

**Events:** `room_created`, `room_joined`, `game_started`, `game_finished` + SPA page views.

**Production setup:**

1. Add `UMAMI_APP_SECRET` and DB vars to `.env` (see `.env.example`).
2. First deploy with empty `pgdata` creates the `umami` database automatically. Existing Postgres: create DB/user manually.
3. Start stack: `docker compose -f docker-compose.prod.yml up -d`.
4. Open Umami (`UMAMI_PORT`, default 3000), sign in (`admin` / `umami`), change password.
5. Add a website with your public URL (`APP_PUBLIC_URL`), copy **Website ID**.
6. Set build-time vars and rebuild the app image:
   - `VITE_UMAMI_SCRIPT_URL=https://analytics.example.com/script.js`
   - `VITE_UMAMI_WEBSITE_ID=<uuid from dashboard>`
7. Put the same vars in GitHub Actions secrets for release builds (`VITE_PUBLIC_URL`, `VITE_UMAMI_*`).

Use a subdomain (e.g. `analytics.example.com`) with HTTPS in front of Umami port 3000.

## Health & API

- `GET /health` — liveness
- `GET /health/ready` — DB ready
- `GET /swagger-ui` — when `app.expose-api-docs=true`
- `GET/POST /api/*` — JSON REST for SPA (rooms, session, locale, i18n)
- `GET /ws/rooms/{code}` — real-time game state

## Architecture

```
com.lalkalol/
├── common/model/   shared enums (Team, Role, Language, RoomStatus)
├── game/           board, turns, game state, dto (game view, WS client messages)
├── room/           lobby, players, lifecycle, dto, web (REST, WebSocket)
├── words/          dictionary seed + lookup
├── db/             JPA entities and repositories
├── i18n/           messages, dto, web (locale API)
├── web/            session, SPA, security, shared error DTOs
└── config/         Spring config, health endpoints
```

- Room and game state in PostgreSQL (JPA); **Flyway** migrations on startup.
- Real-time updates via **WebSockets**; fan-out is **in-memory** on one process — scale out only after shared pub/sub (e.g. Redis).
- SPA served from `static/`; landing prerendered with **vite-ssg** for SEO.
