FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# The backend jar is packaged by the CI/local Maven step before `docker build backend`.
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
