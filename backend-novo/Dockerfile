FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
# Unit and PostgreSQL integration tests are run by mvn verify before delivery.
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:17-jre-jammy AS runtime
RUN groupadd --gid 10001 kaup && useradd --uid 10001 --gid kaup --no-create-home kaup \
    && apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build --chown=kaup:kaup /build/target/kaup-backend-novo.jar app.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
    CMD curl --fail --silent http://localhost:8080/api/analise/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
