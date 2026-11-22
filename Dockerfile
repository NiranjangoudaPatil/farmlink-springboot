# Two stages: build the jar with Maven, then run it on a small Java runtime image.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Uploaded product images are written here. On a host, mount a persistent volume on /data
# (and set UPLOAD_DIR=/data/uploads), otherwise images vanish whenever the container restarts.
ENV UPLOAD_DIR=/data/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
