## Backend runtime-only Dockerfile
# 1. On host (Droplet) build the jar first:
#    cd backend && ./gradlew clean bootJar
# 2. Then build this image (no Gradle/JDK heavy build in container):
#    docker build -t flip-da-table-backend ./backend

FROM eclipse-temurin:17-jre-jammy
LABEL org.opencontainers.image.source="https://github.com/Anguy491/flip-da-table" \
      org.opencontainers.image.title="flip-da-table-backend" \
      org.opencontainers.image.description="Runtime image for Flip Da Table Spring Boot backend (pre-built jar)" \
      org.opencontainers.image.licenses="Apache-2.0"

ARG JAR_FILE=build/libs/*SNAPSHOT.jar
WORKDIR /app

# Copy the already-built jar from the build context
COPY ${JAR_FILE} app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+HeapDumpOnOutOfMemoryError"

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]

# Notes:
# - If the jar name changes (non-SNAPSHOT) you can pass --build-arg JAR_FILE=build/libs/backend-0.0.1.jar
# - Consider adding a .dockerignore to exclude src/ and Gradle files to shrink context once this flow is stable.
