package de.studyplanner;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integrationstests für Registrierung mit E-Mail-Bestätigung, Login (inkl. Sperre nach Fehlversuchen),
 * "Passwort vergessen" und den Schutz der Endpunkte durch JWT.
 */
class AuthIntegrationTest extends IntegrationTestBase {

    /** Zufällige, eindeutige E-Mail-Adresse für einen Test. */
    private static String randomEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@test.de";
    }

    /**
     * Registrieren schickt eine Bestätigungsmail und liefert noch kein Token. Der Klick auf den Link aktiviert
     * das Konto, meldet aber nicht an. Danach ist der Login möglich – auch mit anderer Groß-/Kleinschreibung.
     */
    @Test
    void registerVerifyAndLogin() throws Exception {
        String email = "Max-" + UUID.randomUUID() + "@Test.de";

        register(email, "geheim123", "Max")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", containsString(email.toLowerCase())))
                .andExpect(jsonPath("$.token").doesNotExist());

        assertThat(mailsTo(email)).hasSize(1);
        assertThat(mailsTo(email).get(0).getText()).contains("http://localhost:3000/verify-email?token=");

        // Vor der Bestätigung: richtiges Passwort, aber 403 mit Code für das Frontend
        login(email, "geheim123")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        // Bestätigen meldet nicht an – es gibt nur eine Meldung, danach normaler Login
        verifyEmail(tokenFromLastMailTo(email))
                .andExpect(jsonPath("$.message", containsString("bestätigt")))
                .andExpect(jsonPath("$.token").doesNotExist());

        login(email.toUpperCase(), "geheim123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.displayName").value("Max"));
    }

    /** Ein Bestätigungslink funktioniert nur einmal; ein ausgedachtes Token gar nicht. */
    @Test
    void verificationLinkWorksOnlyOnce() throws Exception {
        String email = randomEmail("einmal");
        register(email, "geheim123", "Max").andExpect(status().isCreated());
        String token = tokenFromLastMailTo(email);

        verifyEmail(token);
        mockMvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"%s\"}".formatted(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"ausgedacht\"}"))
                .andExpect(status().isBadRequest());
    }

    /** "Erneut senden" verschickt nur für unbestätigte Konten eine Mail, antwortet aber immer gleich. */
    @Test
    void resendVerificationMail() throws Exception {
        String email = randomEmail("erneut");
        register(email, "geheim123", "Max").andExpect(status().isCreated());

        resend(email).andExpect(status().isOk());
        // Innerhalb einer Minute wird keine zweite Mail geschickt (Schutz vor Mail-Spam)
        assertThat(mailsTo(email)).hasSize(1);

        String unknown = randomEmail("unbekannt");
        resend(unknown).andExpect(status().isOk());
        assertThat(mailsTo(unknown)).isEmpty();
    }

    /**
     * Doppelte Registrierung verrät nicht, dass die Adresse existiert: gleiche Antwort wie bei einer neuen
     * Adresse. Der Besitzer bekommt stattdessen eine Mail "Du hast bereits ein Konto" mit Reset-Link.
     * Sein Passwort bleibt dabei unverändert.
     */
    @Test
    void registerWithExistingEmailDoesNotRevealAccount() throws Exception {
        String email = randomEmail("doppelt");
        registerVerifiedUser(email, "geheim123");

        String newAddressAnswer = register(randomEmail("neu"), "geheim123", "Max")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String existingAddressAnswer = register(email, "anderesPasswort1", "Max")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat(existingAddressAnswer.replace(email, "X")).isEqualTo(newAddressAnswer.replaceAll("neu-[^ ]+@test.de", "X"));

        assertThat(mailsTo(email).get(mailsTo(email).size() - 1).getSubject()).isEqualTo("Du hast bereits ein Konto");
        assertThat(tokenFromLastMailTo(email)).isNotBlank();
        login(email, "geheim123").andExpect(status().isOk());
        login(email, "anderesPasswort1").andExpect(status().isUnauthorized());
    }

    /** Ist das bestehende Konto noch unbestätigt, kommt (höchstens einmal pro Minute) die Bestätigungsmail erneut. */
    @Test
    void registerAgainResendsVerificationForUnverifiedAccount() throws Exception {
        String email = randomEmail("unbestaetigt");
        register(email, "geheim123", "Max").andExpect(status().isCreated());
        register(email, "geheim123", "Max").andExpect(status().isCreated());

        assertThat(mailsTo(email)).hasSize(1);
        assertThat(mailsTo(email).get(0).getSubject()).isEqualTo("Bitte bestätige deine E-Mail-Adresse");
    }

    /** Ungültige Eingaben → 400 mit einer Fehlermeldung pro Feld. */
    @Test
    void registerWithInvalidDataReturns400WithFieldErrors() throws Exception {
        register("keine-email", "kurz", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email", notNullValue()))
                .andExpect(jsonPath("$.errors.password", notNullValue()))
                .andExpect(jsonPath("$.errors.displayName", notNullValue()));
    }

    /** Falsches Passwort → 401. */
    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        String email = randomEmail("falsch");
        register(email, "geheim123", "Max").andExpect(status().isCreated());
        login(email, "falsches-passwort").andExpect(status().isUnauthorized());
    }

    /** Nach 5 Fehlversuchen ist die Adresse gesperrt (429) – selbst das richtige Passwort hilft dann nicht. */
    @Test
    void loginIsLockedAfterTooManyFailures() throws Exception {
        String email = randomEmail("sperre");
        register(email, "geheim123", "Max").andExpect(status().isCreated());
        verifyEmail(tokenFromLastMailTo(email));

        for (int i = 0; i < 5; i++) {
            login(email, "falsch-" + i).andExpect(status().isUnauthorized());
        }
        login(email, "geheim123")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail", containsString("Minuten")));

        // Auch unbekannte Adressen werden gesperrt – so verrät die Sperre nicht, welche Konten existieren
        String unknown = randomEmail("unbekannt");
        for (int i = 0; i < 5; i++) {
            login(unknown, "falsch").andExpect(status().isUnauthorized());
        }
        login(unknown, "falsch").andExpect(status().isTooManyRequests());
    }

    /**
     * Passwort vergessen: Mail mit Link → neues Passwort setzen → altes Passwort geht nicht mehr,
     * neues schon. Der Link funktioniert nur einmal; unbekannte Adressen bekommen dieselbe Antwort.
     */
    @Test
    void forgotAndResetPassword() throws Exception {
        String email = randomEmail("vergessen");
        register(email, "altesPasswort1", "Max").andExpect(status().isCreated());

        forgot(email).andExpect(status().isOk());
        String resetToken = tokenFromLastMailTo(email);
        assertThat(mailsTo(email).get(mailsTo(email).size() - 1).getText()).contains("/reset-password?token=");

        reset(resetToken, "neuesPasswort1").andExpect(status().isOk());
        reset(resetToken, "nochEinPasswort1").andExpect(status().isBadRequest());

        login(email, "altesPasswort1").andExpect(status().isUnauthorized());
        // Der Reset-Link kam per Mail → die Adresse gilt damit auch als bestätigt
        login(email, "neuesPasswort1").andExpect(status().isOk());

        String unknown = randomEmail("niemand");
        forgot(unknown)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Falls ein Konto")));
        assertThat(mailsTo(unknown)).isEmpty();
    }

    /** Ohne oder mit kaputtem Token → 401. */
    @Test
    void protectedEndpointsNeedToken() throws Exception {
        mockMvc.perform(get("/api/courses")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/courses").header("Authorization", "Bearer kaputt"))
                .andExpect(status().isUnauthorized());
    }

    /** /api/auth/me liefert den Benutzer zum Token. */
    @Test
    void meReturnsCurrentUser() throws Exception {
        String token = registerUser();
        getJson("/api/auth/me", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Test User"));
    }

    /** POST /api/auth/resend-verification */
    private ResultActions resend(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\"}".formatted(email)));
    }

    /** POST /api/auth/forgot-password */
    private ResultActions forgot(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\"}".formatted(email)));
    }

    /** POST /api/auth/reset-password */
    private ResultActions reset(String token, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"%s\", \"password\": \"%s\"}".formatted(token, password)));
    }
}
