FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 serviceevidence && mkdir -p /app/private-evidence && chown -R serviceevidence /app
COPY build/libs/*.jar /app/app.jar
USER serviceevidence
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
