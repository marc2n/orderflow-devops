FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the already-built jar from the service's target directory (build artifacts are uploaded/downloaded between jobs)
COPY target/*jar notification-service.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "notification-service.jar"]