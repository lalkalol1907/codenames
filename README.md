# Codenames Online

Multiplayer word-deduction game in the browser (Codenames-style).

| | |
|---|---|
| **Backend** | Ktor 3 · Kotlin · JSON API · WebSockets |
| **Frontend** | Vue 3 · TypeScript · Vite · vite-ssg |
| **Database** | PostgreSQL (Docker/prod) · H2 (local `./gradlew run`) |

## Requirements

- JDK **24**
- **Node.js 26.3.0** (see [`.nvmrc`](.nvmrc)) and **pnpm 10**
- Docker — optional (production-like stack)

## Local development

Backend:

```bash
./gradlew run    # → http://localhost:8080
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

Production-like UI from Ktor only: run `pnpm build` in `frontend/`, then `./gradlew run`.

## Docker

```bash
cp .env.example .env
# Set SESSION_SECRET, APP_PUBLIC_URL, and DB credentials

docker compose up --build
# Production + HTTPS (needs ./certs/keystore.p12):
# docker compose -f docker-compose.prod.yml up -d
```

**HTTPS keystore** (once per cert):

```bash
SSL_KEYSTORE_PASSWORD=… ./scripts/pem-to-pkcs12.sh           # Let's Encrypt PEM → PKCS12
SSL_KEYSTORE_PASSWORD=… ./scripts/generate-dev-certs.sh    # self-signed (local)
```

Config profiles: `application-prod.yaml` (HTTP) · `application-prod-https.yaml` (8080 + 443).

## Environment

| Variable | Description |
|----------|-------------|
| `DATABASE_*` | JDBC URL, user, password, driver |
| `SESSION_SECRET` | Cookie signing (32+ random chars) |
| `APP_PUBLIC_URL` | Public site URL |
| `APP_ENV` | `dev` \| `prod` |
| `APP_SECURE_COOKIES` | `true` behind HTTPS |
| `APP_EXPOSE_API_DOCS` | OpenAPI at `/openapi` (default off in prod) |
| `SSL_*`, `HTTP_PORT`, `HTTPS_PORT` | Keystore under `SSL_CERTS_DIR` (default `./certs`) |
| `VITE_PUBLIC_URL` | Canonical URL baked into frontend SEO meta at build time |

Full template: [`.env.example`](.env.example).

## Health & API

- `GET /health` — liveness  
- `GET /health/ready` — DB ready  
- `GET /openapi` — when API docs are enabled  
- `GET/POST /api/*` — JSON REST for SPA (rooms, session, locale, i18n)
- `GET /ws/rooms/{code}` — real-time game state

## Architecture

- Room and game state in PostgreSQL (Exposed); **Flyway** migrations on startup.
- Real-time updates via **WebSockets**; fan-out is **in-memory** on one process — scale out only after shared pub/sub (e.g. Redis).
- SPA served from `static/`; landing prerendered with **vite-ssg** for SEO.
- OpenAPI/AsyncAPI in dev when `app.exposeApiDocs=true`.
