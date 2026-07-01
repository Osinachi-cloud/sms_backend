# ----------------------------------------------
# SchoolSaaS Backend — Production Dockerfile
# Optimized multi-stage build for Railway / VPS
# Using Java 25
# ----------------------------------------------

# Stage 1: Build
FROM openjdk:25-slim AS builder
WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy POM first (cache layer)
COPY pom.xml .

# Download dependencies (cached if pom.xml unchanged)
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM openjdk:25-slim
WORKDIR /app

# Create non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy the Spring Boot fat JAR
COPY --from=builder /app/target/*.jar app.jar

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

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]