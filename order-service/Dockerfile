FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the already-built jar from the service's target directory (build artifacts are uploaded/downloaded between jobs)
COPY target/*jar order-service.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "order-service.jar"]