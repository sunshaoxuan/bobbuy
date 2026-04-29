FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# The backend jar is prepared by `mvn -DskipTests package` before `docker build backend -t bobbuy-backend-test` from the repo root.
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
