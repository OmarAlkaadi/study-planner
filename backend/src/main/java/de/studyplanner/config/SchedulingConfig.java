package de.studyplanner.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Schaltet geplante Aufgaben (@Scheduled) ein: das nächtliche Aufräumen (AccountCleanupJob) und
 * das nächtliche Zurücksetzen der Demo-Daten (DemoDataInitializer).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
