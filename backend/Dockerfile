# ---- Build: kompiliert das Projekt mit Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Erst nur die pom.xml kopieren -> Abhängigkeiten werden gecacht,
# solange sich die pom.xml nicht ändert
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Run: schlankes Image nur mit Java-Runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Europe/Berlin

# Nicht als root laufen
RUN useradd --system --uid 1001 spring
USER spring

COPY --from=build /app/target/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
