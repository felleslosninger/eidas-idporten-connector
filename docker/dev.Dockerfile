FROM maven:3.9-eclipse-temurin-25 AS builder

ARG GIT_PACKAGE_TOKEN
ARG GIT_PACKAGE_USERNAME

ENV GIT_PACKAGE_TOKEN=${GIT_PACKAGE_TOKEN}
ENV GIT_PACKAGE_USERNAME=${GIT_PACKAGE_USERNAME}

COPY docker/settings.xml /root/.m2/settings.xml

WORKDIR /home/app
COPY pom.xml .
COPY src ./src

RUN --mount=type=cache,target=/root/.m2/repository mvn -B package  dependency:go-offline -Dmaven.gitcommitid.skip=true -Dmaven.test.skip=true
# -Dmaven.test.skip=true

RUN curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar 

FROM eclipse-temurin:25-jre-noble

ARG APPLICATION=eidas-idporten-connector
RUN mkdir /var/log/${APPLICATION}
RUN mkdir /usr/local/webapps
WORKDIR /usr/local/webapps

COPY --from=builder /home/app/target/${APPLICATION}-DEV-SNAPSHOT.jar application.jar
COPY --from=builder /home/app/opentelemetry-javaagent.jar .

ENV TZ=Europe/Oslo
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 8080
