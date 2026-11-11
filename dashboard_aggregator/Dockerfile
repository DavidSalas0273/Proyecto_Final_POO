# ---------- Etapa 1: Build / Compilación ----------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

# Caché de capas: primero solo el pom.xml y descarga offline de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Luego el código fuente y empaquetado (sin pruebas para agilizar CI/CD)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- Etapa 2: Runtime ligero y seguro ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Principio de menor privilegio: usuario de sistema no privilegiado
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copia exclusiva del artefacto compilado
COPY --from=build --chown=appuser:appgroup /workspace/target/app.jar /app/app.jar

# Ajustes ergonómicos de la JVM para contenedores (cgroups)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

USER appuser
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
