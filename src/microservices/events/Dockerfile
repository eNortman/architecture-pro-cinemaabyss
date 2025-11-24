FROM gradle:9.2.0-jdk17-ubi AS cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME=/home/gradle/cache_home
COPY build.gradle.kts /home/gradle/app/
COPY gradle /home/gradle/app/gradle
WORKDIR /home/gradle/app
RUN gradle dependencies --no-daemon

FROM gradle:9.2.0-jdk17-ubi AS build
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY --chown=gradle:gradle.kts . /home/gradle/src
WORKDIR /home/gradle/src

RUN gradle clean build -x test --no-daemon

# FROM eclipse-temurin:17-jdk-noble AS runtime
FROM openjdk:17-ea-jdk-alpine AS runtime
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/cinemaevents.jar
ENTRYPOINT ["java","-jar","/app/cinemaevents.jar"]
