FROM eclipse-temurin:24-jdk AS build
WORKDIR /app

COPY gradlew gradlew.bat gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY src/ src/

RUN chmod +x gradlew \
    && ./gradlew installDist --no-daemon -x test

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
