# Etapa 1: compila a aplicação
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src
RUN chmod +x gradlew && ./gradlew --no-daemon bootJar

# Etapa 2: imagem final só com o JRE e o jar
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/build/libs/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]