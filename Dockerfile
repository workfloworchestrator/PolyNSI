FROM maven:3-eclipse-temurin-21@sha256:9ba6dd60aae02767cfd34403fd85b77cde495dfa7eaef774a2490104d1124c64 AS build
ARG VERSION
WORKDIR /usr/local/src/polynsi
COPY pom.xml .
COPY src src
RUN mvn versions:set -DnewVersion="${VERSION:?VERSION build argument is required}" -DgenerateBackupPoms=false \
    && mvn clean package

FROM gcr.io/distroless/java21@sha256:27d6932e85923aa9baf382f3daed5a587fe764c4c5397a0fa085a3f1b8f637ec
WORKDIR /usr/local/polynsi
COPY --from=build /usr/local/src/polynsi/target/*.jar polynsi.jar
USER nobody
EXPOSE 8080/tcp 9090/tcp
ENTRYPOINT []
CMD ["java", "-jar", "polynsi.jar"]
