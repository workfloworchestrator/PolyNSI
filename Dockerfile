# The jar is platform-independent, so build it once natively instead of under QEMU per platform.
FROM --platform=$BUILDPLATFORM maven:3-eclipse-temurin-21@sha256:d49bca545509b98965a808175862337dfdd24e1a8a4b11005cc8ce0578e52e8c AS build
ARG VERSION
WORKDIR /usr/local/src/polynsi
COPY pom.xml .
COPY src src
RUN mvn versions:set -DnewVersion="${VERSION:?VERSION build argument is required}" -DgenerateBackupPoms=false \
    && mvn clean package

FROM gcr.io/distroless/java21@sha256:3784928ea4f476b7f4f4d349f0439d208605ecb6883abd1186308ed7e2b6a31e
WORKDIR /usr/local/polynsi
COPY --from=build /usr/local/src/polynsi/target/*.jar polynsi.jar
USER nobody
EXPOSE 8080/tcp 9090/tcp
ENTRYPOINT []
CMD ["java", "-jar", "polynsi.jar"]
