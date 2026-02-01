# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy source code
COPY src src

# Make gradlew executable and build
RUN chmod +x gradlew
RUN ./gradlew clean bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built JAR from build stage
COPY --from=build /app/build/libs/gym-management-0.0.1-SNAPSHOT.jar app.jar

# Set ownership to non-root user
RUN chown -R appuser:appgroup /app
USER appuser

# Expose the port (Cloud Run sets PORT env variable, defaults to 8080)
EXPOSE 8080

# Cloud Run: Use shell form to expand $PORT variable
# JVM optimizations for faster startup and containerized environments
ENTRYPOINT exec java \
    -Dserver.port=${PORT:-8080} \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=1 \
    -Xss256k \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.jmx.enabled=false \
    -Dspring.config.location=classpath:/application.properties,classpath:/application-prod.properties \
    -jar app.jar
