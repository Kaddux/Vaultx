FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app
COPY --from=builder /app/target/user-service-0.0.1-SNAPSHOT.jar ./app.jar
EXPOSE 8000 9000
ENTRYPOINT ["java", "-jar", "app.jar"]