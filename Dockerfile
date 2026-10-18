# ─── Stage 1: Build (multi-stage para imagem menor) ───
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Corrigir finais de linha e permissão do Maven Wrapper (Windows → Linux Alpine)
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Baixa dependências antes (cache de camada)
RUN ./mvnw dependency:go-offline -q

COPY src src
# Compila e executa testes unitários (sem -DskipTests). Testes com falha quebram o build.
# Testes de integração (*IT.java) são segregados e não executam nesta fase (sem Docker-in-Docker).
RUN ./mvnw package -DskipITs -q

# ─── Stage 2: Runtime ───
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Execução com usuário non-root para segurança
RUN addgroup -S rotavital && adduser -S rotavital -G rotavital && chown -R rotavital:rotavital /app
USER rotavital

COPY --chown=rotavital:rotavital --from=builder /workspace/target/rota-vital-*.jar app.jar

ENV JAVA_OPTS=""
EXPOSE 8080

# Healthcheck nativo verificando o endpoint do Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Entrypoint consumindo formalmente a variável de ambiente JAVA_OPTS
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
