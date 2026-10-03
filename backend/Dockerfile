FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends fonts-liberation2 fonts-dejavu-core && rm -rf /var/lib/apt/lists/* \
    && groupadd --system utaped && useradd --system --gid utaped utaped \
    && mkdir -p /app/runtime-data/documents && chown -R utaped:utaped /app/runtime-data
COPY --from=build /app/target/utaped-0.0.1-SNAPSHOT.jar app.jar
USER utaped
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
