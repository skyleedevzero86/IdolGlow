FROM eclipse-temurin:25-jre

RUN groupadd --system app && useradd --system --gid app --home-dir /app app
WORKDIR /app

ARG JAR_FILE
COPY ${JAR_FILE} /app/app.jar

USER app

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    SPRING_CONFIG_ADDITIONAL_LOCATION="optional:file:/config/"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
