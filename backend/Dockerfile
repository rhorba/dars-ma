FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

# Not eclipse-temurin:25-jre-alpine: ONNX Runtime's native library (used by the embedding
# provider) is a glibc build and fails to load on musl/Alpine with
# "Error loading shared library libstdc++.so.6: No such file or directory". This base image
# is Ubuntu (glibc), which resolves it without needing musl compatibility shims.
FROM eclipse-temurin:25-jre
RUN apt-get update && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd -r app && useradd -r -g app app
# useradd -r doesn't create a home dir, so $HOME is unset for the app user - the HuggingFace
# tokenizers native library (JNI, used by the embedding provider) defaults to writing its own
# cache under $HOME/.cache and fails with "Permission denied" resolving that against an unset/
# root-only HOME. Point it at /app, which is already chowned to app:app below.
ENV HOME=/app
WORKDIR /app
RUN chown app:app /app
COPY --from=builder --chown=app:app /app/target/*.jar app.jar
# The djl-cache named volume mounts here at runtime; a named volume with no prior content is
# created root-owned unless the image path it overlays already has the right ownership, which
# Docker then copies in on first mount - so this has to exist and be chowned before USER app.
RUN mkdir -p /app/.djl-cache && chown app:app /app/.djl-cache
USER app
HEALTHCHECK --interval=30s CMD wget -qO- http://localhost:8080/actuator/health || exit 1
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
