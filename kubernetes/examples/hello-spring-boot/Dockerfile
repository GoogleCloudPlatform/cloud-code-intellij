FROM maven:3.5.3-jdk-8 AS builder
COPY pom.xml pom.xml
COPY src/ src/
RUN mvn package

FROM openjdk:8-jdk-alpine
VOLUME /tmp
COPY --from=builder target/hello.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
