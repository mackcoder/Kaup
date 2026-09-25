FROM node:18-alpine

WORKDIR /app

EXPOSE 5173

# Mantém o container do frontend ativo sem tentar rodar o npm install
CMD ["tail", "-f", "/dev/null"]