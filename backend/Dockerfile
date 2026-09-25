FROM eclipse-temurin:17-jdk-alpine

# Instala o Tesseract OCR e os dados em Português
RUN apk add --no-cache \
    tesseract-ocr \
    tesseract-ocr-data-por

WORKDIR /app

EXPOSE 8080

# Mantém o container do backend 100% ativo e estável no ambiente
CMD ["tail", "-f", "/dev/null"]