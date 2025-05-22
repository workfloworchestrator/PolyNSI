FROM maven:3.9-amazoncorretto-17-debian AS build
WORKDIR /usr/local/src/polynsi
COPY . .
RUN mvn clean package

#FROM amazoncorretto:17
FROM eclipse-temurin:17-jre-centos7
WORKDIR /usr/local/polynsi
COPY --from=build /usr/local/src/polynsi/target/*.jar polynsi.jar
USER nobody
EXPOSE 8080/tcp 9090/tcp
CMD java -jar polynsi.jar

