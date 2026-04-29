# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
RUN apk add --no-cache ca-certificates && update-ca-certificates
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Stage 2: Runtime ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache ca-certificates && update-ca-certificates
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
