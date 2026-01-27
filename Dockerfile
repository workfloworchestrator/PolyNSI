FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /usr/local/src/polynsi
COPY pom.xml .
COPY src src
RUN mvn clean package

FROM gcr.io/distroless/java21
WORKDIR /usr/local/polynsi
COPY --from=build /usr/local/src/polynsi/target/*.jar polynsi.jar
USER nobody
EXPOSE 8080/tcp 9090/tcp
ENTRYPOINT []
CMD ["java", "-jar", "polynsi.jar"]
