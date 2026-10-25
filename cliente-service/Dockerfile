# ==========================================
# Multi-stage Dockerfile: cliente-service
# ==========================================

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copiar configuraciones y wrapper de Gradle
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Copiar libreria compartida de seguridad y codigo fuente del microservicio
COPY common-security/ common-security/
COPY cliente-service/ cliente-service/

# Asegurar permisos de ejecucion y compilar el jar ejecutable de Spring Boot
RUN chmod +x gradlew && ./gradlew :cliente-service:bootJar -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear grupo y usuario sin privilegios para ejecucion segura
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiar el artefacto JAR empaquetado desde la etapa de compilacion
COPY --from=builder /workspace/cliente-service/build/libs/cliente-service-*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
