FROM maven:3.9.16-eclipse-temurin-21-alpine AS build

WORKDIR /build
COPY pom.xml ./
COPY src/ ./src/
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app
COPY --from=build /build/target/quarkus-app/ ./

EXPOSE 8081

ENTRYPOINT ["java", "-Dquarkus.http.host=0.0.0.0", "-jar", "/app/quarkus-run.jar"]
