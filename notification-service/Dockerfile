FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app

COPY . .
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends --only-upgrade libssl3 openssl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/notification-service/target/*jar notification-service.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "notification-service.jar"]