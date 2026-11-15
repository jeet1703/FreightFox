# Build stage: compile and package the jar (tests are run with `mvnw test`, not during the image build)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Runtime stage: JRE only
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/alumni-search-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
