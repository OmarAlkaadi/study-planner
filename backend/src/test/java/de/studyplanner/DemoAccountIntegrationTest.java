package de.studyplanner;

import de.studyplanner.demo.DemoAccount;
import de.studyplanner.demo.DemoDataInitializer;
import de.studyplanner.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Schutz des öffentlichen Demo-Accounts: Kontodaten gesperrt, keine Login-Sperre, kein "Passwort vergessen",
 * und das nächtliche Zurücksetzen der Demo-Daten. Eigene Testklasse, weil hier die Demo-Daten aktiv sind.
 */
@TestPropertySource(properties = "app.demo-data=true")
class DemoAccountIntegrationTest extends IntegrationTestBase {

    @Autowired
    private DemoDataInitializer demoDataInitializer;

    @Autowired
    private UserRepository userRepository;

    /** Access-Token des Demo-Accounts. */
    private String demoToken() throws Exception {
        return accessToken(login(DemoAccount.EMAIL, DemoAccount.PASSWORD).andExpect(status().isOk()));
    }

    /** Name, Passwort und Konto des Demo-Accounts lassen sich nicht ändern (403); /me meldet demo = true. */
    @Test
    void demoAccountDataIsLocked() throws Exception {
        String token = demoToken();
        getJson("/api/auth/me", token).andExpect(jsonPath("$.demo").value(true));

        putJson("/api/account/profile", token, "{\"displayName\": \"Gehackt\"}")
                .andExpect(status().isForbidden());
        putJson("/api/account/password", token, """
                {"currentPassword": "%s", "newPassword": "anderesPasswort1"}
                """.formatted(DemoAccount.PASSWORD)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/account").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\": \"demo1234\"}"))
                .andExpect(status().isForbidden());

        login(DemoAccount.EMAIL, DemoAccount.PASSWORD).andExpect(status().isOk());
        getJson("/api/auth/me", token).andExpect(jsonPath("$.displayName").value("Demo Student"));
    }

    /** Falsche Passwörter sperren den Demo-Account nicht – sonst könnte jeder die Demo blockieren. */
    @Test
    void failedLoginsDoNotLockDemoAccount() throws Exception {
        for (int i = 0; i < 8; i++) {
            login(DemoAccount.EMAIL, "falsch-" + i).andExpect(status().isUnauthorized());
        }
        login(DemoAccount.EMAIL, DemoAccount.PASSWORD).andExpect(status().isOk());
    }

    /** "Passwort vergessen" und erneute Registrierung schicken für den Demo-Account keine Mail. */
    @Test
    void noMailsForDemoAccount() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\"}".formatted(DemoAccount.EMAIL))).andExpect(status().isOk());
        register(DemoAccount.EMAIL, "geheim123", "Fremd").andExpect(status().isCreated());
        assertThat(mailsTo(DemoAccount.EMAIL)).isEmpty();
    }

    /** Zurücksetzen löscht Änderungen der Besucher und legt die Beispieldaten frisch an. */
    @Test
    void resetRestoresDemoData() throws Exception {
        String token = demoToken();
        long courseId = createCourse(token, "Von Besuchern angelegt", "MONDAY");
        getJson("/api/courses/" + courseId, token).andExpect(status().isOk());

        demoDataInitializer.resetDemoData();

        assertThat(userRepository.findByEmail(DemoAccount.EMAIL)).isPresent();
        String newToken = demoToken();
        getJson("/api/courses?q=Besuchern", newToken).andExpect(jsonPath("$.length()").value(0));
        getJson("/api/courses", newToken).andExpect(jsonPath("$.length()", greaterThan(0)));
    }
}
