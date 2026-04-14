# =========================
# Build stage
# =========================
FROM maven:3.9.12-eclipse-temurin-21 AS build

LABEL maintainer="kousik"

WORKDIR /build

# Copy pom.xml first (dependency cache layer)
COPY pom.xml ./
RUN mvn -B dependency:go-offline

# Copy source
COPY . .

# Build jar (skip tests)
RUN mvn -B clean package -DskipTests


# =========================
# Runtime stage
# =========================
FROM eclipse-temurin:21-jre-jammy

# (Optional) install netcat only if REALLY needed
RUN apt-get update && apt-get install -y --no-install-recommends netcat \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN useradd -ms /bin/bash easynoks

WORKDIR /home/easynoks

# Copy jar (update name if needed)
COPY --from=build /build/target/*.jar app.jar

# Switch user
USER easynoks

# JVM tuned for stable Spring Boot runtime
ENV JAVA_OPTS="-Xms250m -Xmx350m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ExitOnOutOfMemoryError"

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]