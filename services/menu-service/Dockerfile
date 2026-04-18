# syntax=docker/dockerfile:1

# Build stage -- uses Maven image with JDK 17 pre-installed.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline || true
COPY src ./src
RUN mvn -B package -DskipTests

# Runtime stage -- only the fat jar and a JRE.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
