FROM eclipse-temurin:21-jre

WORKDIR /app

COPY notification-service/target/notification-service-*.jar notification-service.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "notification-service.jar"]