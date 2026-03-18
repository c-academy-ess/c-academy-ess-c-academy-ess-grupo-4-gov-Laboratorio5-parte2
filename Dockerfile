# ==========================================
# Stage 1: Build
# ==========================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine

LABEL description="TodoList REST API"
LABEL java.version="21"

WORKDIR /app

# Security: non-root user
RUN addgroup -S javalin && adduser -S javalin -G javalin

# Copiar apenas o JAR executável com dependências
COPY --from=build /build/target/*jar-with-dependencies.jar /app/app.jar

# Ownership
RUN chown -R javalin:javalin /app
USER javalin

ENTRYPOINT ["java", "-jar", "/app/app.jar"]