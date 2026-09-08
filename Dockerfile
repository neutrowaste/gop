# ==============================================================================
# GOP - Gerenciamento Operacional de Plataforma
# Dockerfile Multi-Stage para Easypanel / Docker Engine
# ==============================================================================

# Argumentos globais de build suportados pelo Easypanel
ARG GIT_SHA=""
ARG GOP_API_URL=""
ARG GOP_CORS_ORIGINS=""
ARG GOP_CORS_ORIGIN_PATTERNS=""
ARG SPRING_DATASOURCE_URL=""
ARG SPRING_DATASOURCE_USERNAME=""
ARG SPRING_DATASOURCE_PASSWORD=""
ARG JWT_SECRET=""
ARG GOP_SEED_ADMIN_ENABLED=""
ARG GOP_SEED_ADMIN_EMAIL=""
ARG GOP_SEED_ADMIN_SENHA=""
ARG GOP_SEED_ADMIN_NOME=""

# ------------------------------------------------------------------------------
# Estágio 1: Build do Frontend Web (Angular 21)
# ------------------------------------------------------------------------------
FROM node:22-alpine AS web-builder
WORKDIR /app/web

COPY web/package.json web/package-lock.json ./
RUN npm ci

COPY web/ ./
RUN npm run build -- --configuration production

# ------------------------------------------------------------------------------
# Estágio 2: Build do Backend API (Spring Boot 3.5 / Java 21)
# ------------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS api-builder
WORKDIR /app/api

COPY api/pom.xml ./
# Cache de dependências Maven
RUN mvn dependency:go-offline -B || true

COPY api/src ./src
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Estágio 3: Runtime de Produção (Nginx + Eclipse Temurin 21 JRE)
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner

# Instalação do Nginx, bash, curl e utilitário dos2unix
RUN apk add --no-cache nginx bash curl dos2unix \
    && mkdir -p /run/nginx /usr/share/nginx/html /app

# Copia artefatos compilados do Angular
COPY --from=web-builder /app/web/dist/web/browser/ /usr/share/nginx/html/

# Copia o JAR do Spring Boot compilado
COPY --from=api-builder /app/api/target/*.jar /app/app.jar

# Copia configuração do Nginx e script de entrypoint
COPY docker/nginx.conf /etc/nginx/nginx.conf
COPY docker/entrypoint.sh /app/entrypoint.sh

# Garante finais de linha Unix (LF) e permissão de execução
RUN dos2unix /app/entrypoint.sh && chmod +x /app/entrypoint.sh

# Configurações padrão de ambiente
ENV SPRING_PROFILES_ACTIVE=postgres \
    PORT=80

EXPOSE 80

ENTRYPOINT ["/app/entrypoint.sh"]
