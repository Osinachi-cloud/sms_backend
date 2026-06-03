# ----------------------------------------------
# SchoolSaaS Backend — Production Dockerfile
# Optimized multi-stage build for Railway / VPS
# ----------------------------------------------

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Install dependencies for build
RUN apk add --no-cache bash

# Copy Maven wrapper and POM first (cache layer)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached if pom.xml unchanged)
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src src
RUN ./mvnw clean package -DskipTests -B && \
    mkdir -p target/dependency && \
    (cd target/dependency; jar -xf ../*.jar)

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Install curl for health checks
RUN apk add --no-cache curl

# Copy layered JAR contents from builder
COPY --from=builder /app/target/dependency/BOOT-INF/lib lib/
COPY --from=builder /app/target/dependency/META-INF META-INF/
COPY --from=builder /app/target/dependency/BOOT-INF/classes classes/

# Set ownership
RUN chown -R spring:spring /app
USER spring

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:InitialRAMPercentage=50.0 -XX:MaxRAMPercentage=75.0 -XX:+OptimizeStringConcat -XX:+UseStringDeduplication"

# Spring profile
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -cp classes:lib/* com.schoolsaas.SchoolSaasApplication"]
