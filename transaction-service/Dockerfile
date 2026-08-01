FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline dependency:resolve-plugins -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app
COPY --from=builder /app/target/transaction-service-0.0.1-SNAPSHOT.jar ./app.jar
EXPOSE 8002 9002
ENTRYPOINT ["java", "-jar", "app.jar"]
