# ARGs that feed into FROM must be declared before the first FROM (global scope).
ARG JRE_IMAGE=amazoncorretto:8

# ── build stage ──────────────────────────────────────────────────────────────
# gradle:7.6.3-jdk8 — Gradle pre-installed, multi-arch, no apt-get required.
FROM gradle:7.6.3-jdk8 AS build
WORKDIR /app
COPY settings.gradle build.gradle ./
RUN gradle dependencies --no-daemon -q
COPY src/ src/
RUN gradle shadowJar --no-daemon -q

# ── runtime stage ─────────────────────────────────────────────────────────────
ARG JRE_IMAGE
FROM ${JRE_IMAGE}
WORKDIR /app

RUN mkdir -p /dumps

COPY --from=build /app/build/libs/file-processor-*.jar app.jar

ENV JAVA_XMS=256m
ENV JAVA_XMX=512m
ENV JAVA_EXTRA_OPTS=""

ENTRYPOINT ["sh", "-c", \
    "exec java \
      -Xms${JAVA_XMS} \
      -Xmx${JAVA_XMX} \
      -XX:+HeapDumpOnOutOfMemoryError \
      -XX:HeapDumpPath=/dumps/heapdump.hprof \
      -XX:+ExitOnOutOfMemoryError \
      ${JAVA_EXTRA_OPTS} \
      -jar /app/app.jar"]