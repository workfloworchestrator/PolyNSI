FROM maven:3-eclipse-temurin-21@sha256:52182d56c0cf03e906a4503a77de81323706e57f0cb4ed3acbfa1054cbff1bae AS build
WORKDIR /usr/local/src/polynsi
COPY pom.xml .
COPY src src
RUN mvn clean package

FROM gcr.io/distroless/java21@sha256:46918c99fec3a4fb69c5e6d0679883935997f63ad602165369795039875384b0
WORKDIR /usr/local/polynsi
COPY --from=build /usr/local/src/polynsi/target/*.jar polynsi.jar
USER nobody
EXPOSE 8080/tcp 9090/tcp
ENTRYPOINT []
CMD ["java", "-jar", "polynsi.jar"]
