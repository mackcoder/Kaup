FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine

# Instala o Tesseract OCR e os dados em Português
RUN apk add --no-cache \
    tesseract-ocr \
    tesseract-ocr-data-por

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

# Mantém o container do backend 100% ativo e estável no ambiente
CMD ["java", "-jar", "app.jar"]
