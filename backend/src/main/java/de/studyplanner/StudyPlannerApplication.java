package de.studyplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Einstiegspunkt des Backends (Spring Boot).
 *
 * @SpringBootApplication aktiviert die Auto-Konfiguration und den Component-Scan: Alle Klassen im Paket
 * de.studyplanner und seinen Unterpaketen mit @RestController, @Service, @Component, @Configuration
 * sowie alle Repositories werden automatisch gefunden und verbunden (Dependency Injection).
 *
 * Ablauf beim Start:
 * 1. Konfiguration aus application.yml bzw. Umgebungsvariablen lesen (DB_URL, JWT_SECRET, ...)
 * 2. Flyway spielt die SQL-Migrationen aus resources/db/migration in die Datenbank ein
 * 3. DemoDataInitializer legt ggf. den Demo-Account an (nur bei APP_DEMO_DATA=true)
 * 4. Der eingebettete Tomcat-Server nimmt Anfragen auf Port 8080 entgegen
 */
@SpringBootApplication
public class StudyPlannerApplication {

    /** Startet die komplette Anwendung. */
    public static void main(String[] args) {
        SpringApplication.run(StudyPlannerApplication.class, args);
    }
}
