FROM amazoncorretto:17-alpine

WORKDIR /app

COPY target/student-service-1.0.0.jar app.jar

EXPOSE 9095

ENTRYPOINT ["java", "-Dserver.port=9095", "-jar", "app.jar"]