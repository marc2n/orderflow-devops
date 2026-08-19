FROM eclipse-temurin:21-jre

WORKDIR /app

COPY order-service/target/order-service-*.jar /app/app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
