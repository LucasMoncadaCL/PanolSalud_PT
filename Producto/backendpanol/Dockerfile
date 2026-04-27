FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

ARG JOOQ_DB_URL
ARG JOOQ_DB_USER
ARG JOOQ_DB_PASSWORD
ENV JOOQ_DB_URL=${JOOQ_DB_URL}
ENV JOOQ_DB_USER=${JOOQ_DB_USER}
ENV JOOQ_DB_PASSWORD=${JOOQ_DB_PASSWORD}

COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY src src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/backendpanol-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
