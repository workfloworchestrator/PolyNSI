# The jar is platform-independent, so build it once natively instead of under QEMU per platform.
FROM --platform=$BUILDPLATFORM maven:3-eclipse-temurin-21@sha256:c665c1041e0c6e28bc7d1b41254622c79cf0b3ac63b519be8b789b05b6f69661 AS build
ARG VERSION
WORKDIR /usr/local/src/polynsi
COPY pom.xml .
COPY src src
RUN mvn versions:set -DnewVersion="${VERSION:?VERSION build argument is required}" -DgenerateBackupPoms=false \
    && mvn clean package

FROM gcr.io/distroless/java21@sha256:6cc0aa2d30ed370e4e2ff75091bd547e15f9cf9508796ae21dff331b7c406cd9
WORKDIR /usr/local/polynsi
COPY --from=build /usr/local/src/polynsi/target/*.jar polynsi.jar
USER nobody
EXPOSE 8080/tcp 9090/tcp
ENTRYPOINT []
CMD ["java", "-jar", "polynsi.jar"]
