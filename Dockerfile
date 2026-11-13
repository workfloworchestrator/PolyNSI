# The jar is platform-independent, so build it once natively instead of under QEMU per platform.
FROM --platform=$BUILDPLATFORM maven:3-eclipse-temurin-21@sha256:851ccd76d49d5646915645e5fb51e9b00ef53ad3dc68b0e9a2a9846b9c56bde3 AS build
ARG VERSION
WORKDIR /usr/local/src/polynsi
COPY pom.xml .
COPY src src
RUN mvn versions:set -DnewVersion="${VERSION:?VERSION build argument is required}" -DgenerateBackupPoms=false \
    && mvn clean package

FROM gcr.io/distroless/java21@sha256:132eb554519304beb13de16013176efb8aa67bb7bf9e2d1ca340e97e744c3cb3
WORKDIR /usr/local/polynsi
COPY --from=build /usr/local/src/polynsi/target/*.jar polynsi.jar
USER nobody
EXPOSE 8080/tcp 9090/tcp
ENTRYPOINT []
CMD ["java", "-jar", "polynsi.jar"]
