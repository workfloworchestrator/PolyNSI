FROM maven:3-eclipse-temurin-21@sha256:0fbcd7bb49ffc0224413f2681c2cef72b89b6d2bea4f91c8c6de37e9616fcc93 AS build
ARG VERSION
WORKDIR /usr/local/src/polynsi
COPY pom.xml .
COPY src src
RUN mvn versions:set -DnewVersion="${VERSION:?VERSION build argument is required}" -DgenerateBackupPoms=false \
    && mvn clean package

FROM gcr.io/distroless/java21@sha256:26a517c7f7d69a98adab4d1e71d5a3a9f1079c85ac9c4193ce6b6bd3d73496f3
WORKDIR /usr/local/polynsi
COPY --from=build /usr/local/src/polynsi/target/*.jar polynsi.jar
USER nobody
EXPOSE 8080/tcp 9090/tcp
ENTRYPOINT []
CMD ["java", "-jar", "polynsi.jar"]
