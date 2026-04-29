FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# This Docker context expects `target/*.jar` from the preceding `cd backend && mvn -DskipTests package` step.
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
