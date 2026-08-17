FROM maven:3-eclipse-temurin-21@sha256:c07f7ccfb8ca6c9fa29ee523f00afa7d2ca6132c92f8652c4aebb5ee3491f502 AS build
WORKDIR /usr/local/src/polynsi
COPY pom.xml .
COPY src src
RUN mvn clean package

FROM gcr.io/distroless/java21@sha256:5c3795442a2e3a035500f5c643786a4fb9669f4d62c98d3ee23f04b7d05ef422
WORKDIR /usr/local/polynsi
COPY --from=build /usr/local/src/polynsi/target/*.jar polynsi.jar
USER nobody
EXPOSE 8080/tcp 9090/tcp
ENTRYPOINT []
CMD ["java", "-jar", "polynsi.jar"]
