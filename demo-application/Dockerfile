FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY multitenancy-spring-boot-starter/pom.xml multitenancy-spring-boot-starter/pom.xml
COPY demo-application/pom.xml demo-application/pom.xml

RUN mvn -pl demo-application -am dependency:go-offline

COPY multitenancy-spring-boot-starter/src multitenancy-spring-boot-starter/src
COPY demo-application/src demo-application/src

RUN mvn -pl demo-application -am clean package -DskipTests

FROM eclipse-temurin:17-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /workspace/demo-application/target/demo-application-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
