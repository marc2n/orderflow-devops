# Build stage
# Use Maven image with Temurin JDK 21 for building
FROM eclipse-temurin:21-jre AS build
# Set working directory for build
WORKDIR /app

# Copy pom to download dependencies
COPY pom.xml .
# Pre-fetch dependencies to speed up build
RUN mvn dependency:go-offline

# Copy source code into image
COPY src ./src
# Build application JAR, skipping tests
RUN mvn clean package -DskipTests

# Run Stage
# Runtime image with Corretto JDK 21 (Alpine)
FROM eclipse-temurin:21-jre
# Set working directory in runtime image
WORKDIR /app
# Copy built JAR from build stage and name it notification-service.jar
COPY --from=build /app/target/*jar notification-service.jar
# Expose application port
EXPOSE 8082
# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "notification-service.jar"]
