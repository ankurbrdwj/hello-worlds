# ── build stage ──────────────────────────────────────────────────────────────
# Spring Boot 4.x requires Java 17+; Java 8 (1.8.0_492) is incompatible.
# For a Java-8-compatible cache service downgrade to spring-boot 2.7.x.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -q

# ── runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN mkdir -p /dumps
COPY --from=build /app/build/libs/*.jar app.jar

ENV JAVA_XMS=256m
ENV JAVA_XMX=512m
ENV JAVA_EXTRA_OPTS=""

EXPOSE 8080

ENTRYPOINT ["sh", "-c", \
    "exec java \
      -Xms${JAVA_XMS} \
      -Xmx${JAVA_XMX} \
      -XX:+HeapDumpOnOutOfMemoryError \
      -XX:HeapDumpPath=/dumps/heapdump.hprof \
      -XX:+ExitOnOutOfMemoryError \
      ${JAVA_EXTRA_OPTS} \
      -jar /app/app.jar"]