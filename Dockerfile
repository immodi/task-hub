# syntax=docker/dockerfile:1

# --- build stage -----------------------------------------------------------
# The project targets Java 27; bump/lower this tag together with <java.version> in pom.xml.
FROM maven:3.9-eclipse-temurin-27 AS build
WORKDIR /workspace

# Resolve dependencies first so they are cached when only the sources change.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# --- runtime stage ---------------------------------------------------------
FROM eclipse-temurin:27-jre
WORKDIR /app
COPY --from=build /workspace/target/taskhub-*.jar app.jar

EXPOSE 9090
ENV PORT=9090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
