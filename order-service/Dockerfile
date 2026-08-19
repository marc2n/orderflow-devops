FROM eclipse-temurin:21-jre

WORKDIR /app

COPY order-service/target/order-service-*.jar order-service.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "order-service.jar"]