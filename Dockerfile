####################################### Build stage #######################################
FROM maven:3.9-eclipse-temurin-21-alpine AS build-stage

ARG PROFILE

COPY pom.xml /build/
COPY core /build/core/
COPY web /build/web/
COPY settings.xml /root/.m2/settings.xml
RUN rm -f /build/web/src/main/resources/config/app-*.env
RUN rm -f /build/web/src/main/resources/logging/logback-*.xml

WORKDIR /build/
RUN mvn dependency:go-offline
RUN mvn clean package
######################################## Run Stage ########################################
# glibc-based runtime: netty's native libs (quic/http3, epoll) are built for glibc and
# require libgcc_s.so.1, which the musl-based alpine image lacks. Using the Debian/Ubuntu
# temurin image avoids UnsatisfiedLinkError when reactor-netty initializes the HTTPS client.
FROM eclipse-temurin:21-jre

ARG PROFILE
ENV SERVER_PORT=8080
EXPOSE ${SERVER_PORT}

COPY --from=build-stage /build/web/target/repository-deposit-web-1.0.0-SNAPSHOT.jar /app/repository-deposit-web.jar

ENTRYPOINT ["java","-Dspring.config.additional-location=file:/config/","-Dspring.profiles.active=${PROFILE}","-Djava.security.egd=file:/dev/./urandom","-jar","/app/repository-deposit-web.jar"]