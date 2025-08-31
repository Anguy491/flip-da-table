## Backend Dockerfile (multi-stage) for Spring Boot app
# Build stage
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY src ./src
# Use --no-daemon for reproducibility
RUN ./gradlew clean bootJar --no-daemon

# Runtime stage (distroless-friendly slim JRE)
FROM eclipse-temurin:17-jre-jammy AS runtime
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+HeapDumpOnOutOfMemoryError"
WORKDIR /app
COPY --from=build /app/build/libs/*SNAPSHOT.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
