FROM gradle:jdk-21-and-22 AS build

COPY --chown=gradle:gradle . /home/build
WORKDIR /home/build

RUN ["./gradlew",  "--no-daemon", "clean", "shadowJar"]

FROM openjdk:24

COPY --from=build /home/build/build/libs/*-all.jar /usr/local/share/watson.jar