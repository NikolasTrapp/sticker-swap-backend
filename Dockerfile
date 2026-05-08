# ── Stage 1: Build ──────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /app

# Copy Gradle wrapper and build files first (layer caching)
COPY gradlew ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Download dependencies (cached unless build files change)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon 2>/dev/null || true

# Copy source code
COPY src/ src/

# Build the fat JAR (skip tests — they run in CI)
RUN ./gradlew bootJar --no-daemon -x test

# ── Stage 2: Runtime ───────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

RUN chown -R app:app /app
USER app

EXPOSE 8080

# JVM flags optimized for containers / EC2 spot instances:
#   - UseContainerSupport: respect cgroup memory/cpu limits
#   - MaxRAMPercentage: use up to 75% of container memory for heap
#   - urandom: faster startup (avoid entropy starvation)
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
