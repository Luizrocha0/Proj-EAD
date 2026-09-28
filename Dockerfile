# ─── Stage 1: Build (multi-stage para imagem menor) ───
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Baixa dependências antes (cache de camada)
RUN ./mvnw dependency:go-offline -q

COPY src src
RUN ./mvnw package -DskipTests -q

# ─── Stage 2: Runtime ───
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Não rodar como root
RUN addgroup -S rotavital && adduser -S rotavital -G rotavital
USER rotavital

COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
