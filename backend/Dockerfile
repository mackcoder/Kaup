FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-noble

# Tesseract 5 + idioma português + bibliotecas que o Tess4J carrega
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        tesseract-ocr tesseract-ocr-por libtesseract-dev \
    && rm -rf /var/lib/apt/lists/*

# Sem root: se algo no processo for explorado, não há poderes de administrador no container.
RUN useradd --system --uid 10001 --no-create-home kaup

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER kaup
EXPOSE 8080

# A JVM usa até 75% da memória do container; se faltar mesmo assim (ex.: PDF hostil), ela encerra e o
# Docker reinicia só o backend (restart: unless-stopped), em vez de o servidor inteiro travar.
CMD ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
