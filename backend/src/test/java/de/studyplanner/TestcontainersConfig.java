package de.studyplanner;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Startet für die Integrationstests ein echtes PostgreSQL (gleiche Version wie in docker-compose.yml)
 * in einem Docker-Container. @ServiceConnection trägt Adresse, Benutzer und Passwort automatisch als
 * Datenbankverbindung ein. Spring startet den Container einmal pro Test-Kontext und beendet ihn danach.
 * Flyway legt das Schema aus den echten Migrationen an – so werden auch sie bei jedem Testlauf geprüft.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    /** PostgreSQL-Container für die Tests. */
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    }
}
