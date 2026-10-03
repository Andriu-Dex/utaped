FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system utaped && useradd --system --gid utaped utaped
COPY --from=build /app/target/utaped-0.0.1-SNAPSHOT.jar app.jar
USER utaped
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
