FROM maven:3-eclipse-temurin-21@sha256:98819eb3745bd2007c3f1a19b59085c1fa3929aecb7dbfa431dfcf5a4f18ce3c AS build
WORKDIR /usr/local/src/polynsi
COPY pom.xml .
COPY src src
RUN mvn clean package

FROM gcr.io/distroless/java21@sha256:53ce8e6ab58ff683ec8de330610605a8d0a2d12135d3a1b79fe53290eb1999f2
WORKDIR /usr/local/polynsi
COPY --from=build /usr/local/src/polynsi/target/*.jar polynsi.jar
USER nobody
EXPOSE 8080/tcp 9090/tcp
ENTRYPOINT []
CMD ["java", "-jar", "polynsi.jar"]
