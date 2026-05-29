FROM eclipse-temurin:24-jdk AS build
WORKDIR /app

COPY gradlew gradlew.bat gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY src/ src/

RUN chmod +x gradlew \
    && ./gradlew installDist --no-daemon -x test

FROM eclipse-temurin:24-jre
WORKDIR /app

RUN useradd --system --create-home --uid 10001 app
USER app

COPY --from=build --chown=app:app /app/build/install/codenames/ ./

EXPOSE 8080

ENTRYPOINT ["./bin/codenames"]
