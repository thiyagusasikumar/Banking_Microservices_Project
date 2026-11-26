# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21@sha256:8f6ac126f7810bb5549c4cd122d2bf0e9cda5bdeb0838aa928f09e779fd8bef8 AS build
ARG SERVICE
WORKDIR /build
COPY ${SERVICE}/pom.xml ./pom.xml
COPY ${SERVICE}/src ./src
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -ntp -DskipTests package
COPY docker/ContainerHealthcheck.java /healthcheck/ContainerHealthcheck.java
RUN javac -d /healthcheck /healthcheck/ContainerHealthcheck.java

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --gid 10001 banking && useradd --uid 10001 --gid banking --no-create-home banking
COPY --from=build --chown=banking:banking /build/target/*.jar /app/app.jar
COPY --from=build --chown=banking:banking /healthcheck/ContainerHealthcheck.class /app/healthcheck/
USER banking
HEALTHCHECK --interval=30s --timeout=8s --start-period=90s --retries=5 \
    CMD ["java", "-Xmx32m", "-cp", "/app/healthcheck", "ContainerHealthcheck"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
