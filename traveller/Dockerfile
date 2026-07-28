FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY docker-maven-settings.xml .
RUN mvn -s docker-maven-settings.xml -B -DskipTests dependency:go-offline
COPY src src
RUN mvn -s docker-maven-settings.xml -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S traveller && adduser -S traveller -G traveller
COPY --from=build /workspace/target/traveller-1.0.0.jar app.jar
USER traveller
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=prod"]
