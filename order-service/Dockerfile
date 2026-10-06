FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app

COPY . .
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/order-service/target/*jar order-service.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "order-service.jar"]