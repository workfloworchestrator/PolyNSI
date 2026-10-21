# The jar is platform-independent, so build it once natively instead of under QEMU per platform.
FROM --platform=$BUILDPLATFORM maven:3-eclipse-temurin-21@sha256:d23342b1a070a07555f4ef20c085a81aa40e5f5b3288ebf8ddae2f67e9d760c8 AS build
ARG VERSION
WORKDIR /usr/local/src/polynsi
COPY pom.xml .
COPY src src
RUN mvn versions:set -DnewVersion="${VERSION:?VERSION build argument is required}" -DgenerateBackupPoms=false \
    && mvn clean package

FROM gcr.io/distroless/java21@sha256:1c1a93b840a936b5f2d9b1b2275f94c4f3040fb004c554b85dee647fbdbd874e
WORKDIR /usr/local/polynsi
COPY --from=build /usr/local/src/polynsi/target/*.jar polynsi.jar
USER nobody
EXPOSE 8080/tcp 9090/tcp
ENTRYPOINT []
CMD ["java", "-jar", "polynsi.jar"]
