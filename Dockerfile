# ---- Stage 1: build the executable JAR -------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Resolve dependencies first so this layer is cached until pom.xml changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests clean package

# ---- Stage 2: slim runtime image with only the JAR -------------------------
FROM eclipse-temurin:17-jre-alpine

# Run as an unprivileged user, not root
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app

COPY --from=build /app/target/calculator-devops-*.jar app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
