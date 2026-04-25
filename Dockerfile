FROM maven:3-eclipse-temurin-21@sha256:8e7dc4215c70f922e798c9f8aafa0a3734ca386342427b3dbb17cecc4a429c8e AS build
WORKDIR /usr/local/src/polynsi
COPY pom.xml .
COPY src src
RUN mvn clean package

FROM gcr.io/distroless/java21@sha256:bcdca3804642f766b51c188aa1d080a1bf59bb0dc03361d6cba7636b78597d4e
WORKDIR /usr/local/polynsi
COPY --from=build /usr/local/src/polynsi/target/*.jar polynsi.jar
USER nobody
EXPOSE 8080/tcp 9090/tcp
ENTRYPOINT []
CMD ["java", "-jar", "polynsi.jar"]
