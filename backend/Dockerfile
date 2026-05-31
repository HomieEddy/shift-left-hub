# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper and pom first for dependency caching
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw mvnw.cmd ./

# Download dependencies (layer caching — only reruns when pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src src
RUN ./mvnw package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine AS run
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/knowledge-hub-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Healthcheck — requires spring-boot-starter-actuator (added in Plan 02)
HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
