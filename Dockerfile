FROM maven:3-eclipse-temurin-21@sha256:d7e7f57407437c014571f1ad5a9955f03fc3edcb1d964067ef351fa38e798665 AS build
WORKDIR /usr/local/src/polynsi
COPY pom.xml .
COPY src src
RUN mvn clean package

FROM gcr.io/distroless/java21@sha256:9baaee4f212f681317ff7da424561062fec456a7c6ce304d00158c5d4d171c6d
WORKDIR /usr/local/polynsi
COPY --from=build /usr/local/src/polynsi/target/*.jar polynsi.jar
USER nobody
EXPOSE 8080/tcp 9090/tcp
ENTRYPOINT []
CMD ["java", "-jar", "polynsi.jar"]
