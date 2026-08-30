FROM gradle:jdk21 AS build
WORKDIR /workspace

COPY server/gradlew server/build.gradle server/settings.gradle ./server/
COPY server/gradle ./server/gradle
COPY server/src ./server/src
COPY agents/java ./agents/java

WORKDIR /workspace/server
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/server/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
