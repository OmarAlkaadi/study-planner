package de.studyplanner;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Integrationstests für die Seite "Konto": Name ändern, Passwort ändern, Konto löschen. */
class AccountIntegrationTest extends IntegrationTestBase {

    /** Neuer Anzeigename wird gespeichert und von /me geliefert; leerer Name → 400. */
    @Test
    void updateDisplayName() throws Exception {
        String token = registerUser();
        putJson("/api/account/profile", token, "{\"displayName\": \"  Neuer Name  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Neuer Name"));
        getJson("/api/auth/me", token).andExpect(jsonPath("$.displayName").value("Neuer Name"));

        putJson("/api/account/profile", token, "{\"displayName\": \"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.displayName").exists());
    }

    /** Passwort ändern klappt nur mit dem richtigen aktuellen Passwort (sonst 400, nicht 401). */
    @Test
    void changePassword() throws Exception {
        String email = "konto-" + UUID.randomUUID() + "@test.de";
        String token = registerVerifiedUser(email, "altesPasswort1");

        putJson("/api/account/password", token, """
                {"currentPassword": "falsch", "newPassword": "neuesPasswort1"}
                """).andExpect(status().isBadRequest());

        putJson("/api/account/password", token, """
                {"currentPassword": "altesPasswort1", "newPassword": "neuesPasswort1"}
                """).andExpect(status().isNoContent());

        login(email, "altesPasswort1").andExpect(status().isUnauthorized());
        login(email, "neuesPasswort1").andExpect(status().isOk());
    }

    /** Konto löschen entfernt den Benutzer samt Daten; danach ist kein Login mehr möglich. */
    @Test
    void deleteAccountWithAllData() throws Exception {
        String email = "loeschen-" + UUID.randomUUID() + "@test.de";
        String token = registerVerifiedUser(email, "geheim123");
        long courseId = createCourse(token, "Wird gelöscht", "MONDAY");
        postJson("/api/exams", token, """
                {"courseId": %d, "title": "Klausur", "examDate": "2099-01-01T10:00"}
                """.formatted(courseId)).andExpect(status().isCreated());

        // Falsches Passwort → nichts passiert
        deleteWithBody("/api/account", token, "{\"password\": \"falsch\"}").andExpect(status().isBadRequest());
        getJson("/api/courses", token).andExpect(jsonPath("$", hasSize(1)));

        deleteWithBody("/api/account", token, "{\"password\": \"geheim123\"}").andExpect(status().isNoContent());

        login(email, "geheim123").andExpect(status().isUnauthorized());
        getJson("/api/auth/me", token).andExpect(status().isNotFound());
        // Die E-Mail-Adresse ist wieder frei
        register(email, "geheim123", "Neu").andExpect(status().isCreated());
    }

    /** DELETE mit JSON-Body (für "Konto löschen" mit Passwort-Bestätigung). */
    private ResultActions deleteWithBody(String url, String token, String body)
            throws Exception {
        return mockMvc.perform(delete(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
