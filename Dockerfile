FROM node:26.3.0 AS frontend
WORKDIR /app
RUN npm install -g pnpm@10.11.0
COPY frontend/package.json frontend/pnpm-lock.yaml frontend/
COPY src/main/resources/i18n/ src/main/resources/i18n/
COPY frontend/ frontend/
WORKDIR /app/frontend
ENV VITE_PUBLIC_URL=https://example.com
RUN pnpm install --frozen-lockfile && pnpm run build

FROM eclipse-temurin:24-jdk AS build
WORKDIR /app

COPY gradlew gradlew.bat gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY --from=frontend /app/src/main/resources/static/ src/main/resources/static/
COPY src/ src/

RUN chmod +x gradlew \
    && ./gradlew installDist --no-daemon -x test -PskipFrontendBuild

FROM eclipse-temurin:24-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 app
USER app

COPY --from=build --chown=app:app /app/build/install/codenames/ ./

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

ENTRYPOINT ["./bin/codenames"]
