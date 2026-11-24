FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/cards4play-api-1.0.0.jar app.jar
ENV PORT=4567
ENV DATA_FILE=/data/cards4play.json
ENV TASA_DEFECTO=4000
EXPOSE 4567
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
