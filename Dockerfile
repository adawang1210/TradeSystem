# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper and project metadata first for better layer caching
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Copy source code
COPY src src

# Build the Spring Boot fat jar
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY --from=build /app/target/ipo-simulation-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
