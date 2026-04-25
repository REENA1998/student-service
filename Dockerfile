FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/student-service-1.0.0.jar app.jar

EXPOSE 8089

ENTRYPOINT ["java", "-Dserver.port=8089", "-jar", "app.jar"]