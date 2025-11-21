# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/onlinecodeexecuter-0.0.1-SNAPSHOT.jar app.jar

# Install Java compiler for code execution
RUN apt-get update && apt-get install -y openjdk-21-jdk-headless && apt-get clean

# Create temp directory for code execution
RUN mkdir -p /app/temp-code-execution

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]