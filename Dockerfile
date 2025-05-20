FROM eclipse-temurin:24-jdk-alpine

VOLUME /tmp
ARG JAR_FILE=target/coin-sniper-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/app.jar"]
