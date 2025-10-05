# syntax=docker/dockerfile:1

FROM gradle:8.10-jdk17 AS build
ARG VERSION=0.0.1
WORKDIR /home/gradle/project

COPY gradle gradle
COPY gradlew .
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY src src

RUN chmod +x gradlew \
    && ./gradlew clean bootJar -Pversion=${VERSION} --no-daemon

FROM eclipse-temurin:17-jre-jammy
ENV SPRING_PROFILES_ACTIVE=prod
WORKDIR /app

COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
