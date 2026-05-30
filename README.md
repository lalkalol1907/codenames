# Codenames Online

Multiplayer word deduction game in the browser. Backend: **Ktor 3 + Kotlin**, frontend: **FreeMarker + vanilla JS**, database: **PostgreSQL** (H2 for local dev).

## Requirements

- JDK 24
- Docker (optional, for production-like stack)

## Local development

```bash
./gradlew run
```

Open http://localhost:8080

Tests:

```bash
./gradlew test
```

## Production with Docker Compose

1. Copy environment template:

```bash
cp .env.example .env
```

2. Edit `.env` — set `SESSION_SECRET`, `APP_PUBLIC_URL`, and TLS passwords.

3. Prepare HTTPS keystore (once per certificate renewal):

```bash
# Let's Encrypt PEM → PKCS12
SSL_KEYSTORE_PASSWORD=your-secret ./scripts/pem-to-pkcs12.sh

# Or self-signed for local testing
SSL_KEYSTORE_PASSWORD=dev-secret ./scripts/generate-dev-certs.sh
```

4. Start stack:

```bash
# Local HTTP (dev-like)
docker compose up --build

# Production image + HTTPS (requires ./certs/keystore.p12)
docker compose -f docker-compose.prod.yml up -d
```

The app reads `application-prod.yaml` (HTTP) or `application-prod-https.yaml` (HTTP :8080 + HTTPS :443).
Database credentials and secrets come from `.env`.

## Configuration

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | JDBC URL (PostgreSQL in prod) |
| `DATABASE_USER` | Database user |
| `DATABASE_PASSWORD` | Database password |
| `DATABASE_DRIVER` | JDBC driver class |
| `SESSION_SECRET` | Cookie signing secret (32+ chars) |
| `APP_PUBLIC_URL` | Public site URL for SEO/canonical links |
| `APP_ENV` | `dev` or `prod` |
| `APP_SECURE_COOKIES` | `true` behind HTTPS |
| `APP_EXPOSE_API_DOCS` | `false` in production |
| `SSL_KEYSTORE_PASSWORD` | Password for `/certs/keystore.p12` (HTTPS) |
| `SSL_PRIVATE_KEY_PASSWORD` | Optional; defaults to keystore password |
| `SSL_CERTS_DIR` | Host path mounted as `/certs` (default `./certs`) |
| `HTTP_PORT` / `HTTPS_PORT` | Published ports (default `8080` / `443`) |

Health checks:

- `GET /health` — liveness
- `GET /health/ready` — database connectivity

## Architecture notes

- **Single instance** for WebSocket fan-out (`GameSessionHub` is in-memory). Scale horizontally only after adding Redis Pub/Sub or similar.
- Room/game state is stored in PostgreSQL via Exposed.
- Schema migrations run automatically via Flyway on startup.

## API docs

OpenAPI is available at `/openapi` when `app.exposeApiDocs=true` (disabled in production by default).
