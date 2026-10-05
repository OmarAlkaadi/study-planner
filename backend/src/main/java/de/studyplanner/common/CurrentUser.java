package de.studyplanner.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Liefert die ID des aktuell angemeldeten Benutzers aus dem JWT (Claim "sub").
 *
 * Spring Security prüft bei jeder Anfrage das Token und legt das Ergebnis im SecurityContext ab.
 * Alle Services nutzen diese Klasse, um Datenbankabfragen auf den eigenen Benutzer zu beschränken –
 * so sieht jeder nur seine eigenen Kurse, Klausuren, Aufgaben und Lerneinheiten.
 */
@Component
public class CurrentUser {

    /**
     * Gibt die Benutzer-ID zurück. Ohne gültiges JWT wird eine IllegalStateException geworfen
     * (kommt praktisch nicht vor, weil SecurityConfig solche Anfragen schon vorher mit 401 abweist).
     */
    public Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return Long.valueOf(jwt.getSubject());
        }
        throw new IllegalStateException("Kein angemeldeter Benutzer vorhanden");
    }
}
