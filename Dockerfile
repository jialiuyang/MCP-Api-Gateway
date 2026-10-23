# Single-stage Dockerfile. Assumes the jar has been produced by `mvn package`
# on the host (the standard CI workflow). For a multi-stage build that also
# runs Maven, see Dockerfile.full.
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Application data (H2 file, logs).
RUN mkdir -p /app/data

COPY mcpg-web/target/mcpg-web.jar /app/mcpg-web.jar

EXPOSE 8088

ENV MCPG_PROFILE=dev \
    MCPG_PORT=8088 \
    MCPG_SECURITY_ENABLED=true \
    MCPG_H2_CONSOLE=false \
    MCPG_SSRF_ALLOW_PRIVATE=true \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=70.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/mcpg-web.jar"]
