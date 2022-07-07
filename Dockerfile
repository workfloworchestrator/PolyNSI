FROM maven:3.8.5-openjdk-11 AS build
WORKDIR /usr/local/src/polynsi
COPY . .
RUN mvn clean package

FROM openjdk:11-jre-slim-buster
WORKDIR /usr/local/src/polynsi
COPY --from=build /usr/local/src/polynsi/target/*.jar polynsi.jar
EXPOSE 8080/tcp 9090/tcp
CMD java -jar polynsi.jar

