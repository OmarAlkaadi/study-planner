package de.studyplanner;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integrationstests für die Refresh-Tokens: httpOnly-Cookie beim Login, Erneuern mit Rotation,
 * Erkennung gestohlener (wiederverwendeter) Tokens, Abmelden und Abmelden aller Geräte nach Passwortänderung.
 * In application-test.yml ist die Kulanzzeit für parallele Tabs auf 0 gesetzt (siehe RefreshTokenGraceIntegrationTest).
 */
class RefreshTokenIntegrationTest extends IntegrationTestBase {

    private static String randomEmail() {
        return "refresh-" + UUID.randomUUID() + "@test.de";
    }

    /** Login setzt das Refresh-Token als httpOnly-Cookie mit SameSite=Strict – im JSON steht es nicht. */
    @Test
    void loginSetsHttpOnlyRefreshCookie() throws Exception {
        String email = randomEmail();
        registerVerifiedUser(email, "geheim123");

        ResultActions result = login(email, "geheim123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());

        String header = result.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(header)
                .startsWith("refresh_token=")
                .contains("HttpOnly", "SameSite=Strict", "Path=/api", "Max-Age=2592000");
        assertThat(refreshCookie(result)).hasSizeGreaterThan(30);
    }

    /** Erneuern liefert ein neues Access-Token und ein neues Refresh-Token; das neue Access-Token funktioniert. */
    @Test
    void refreshRotatesTokens() throws Exception {
        String email = randomEmail();
        registerVerifiedUser(email, "geheim123");
        String first = refreshCookie(login(email, "geheim123"));

        ResultActions refreshed = refresh(first)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email));
        String second = refreshCookie(refreshed);
        assertThat(second).isNotBlank().isNotEqualTo(first);

        getJson("/api/auth/me", accessToken(refreshed)).andExpect(status().isOk());
        refresh(second).andExpect(status().isOk());
    }

    /**
     * Wird ein bereits ersetztes Refresh-Token noch einmal benutzt, wurde es vermutlich gestohlen:
     * Dann sind auch alle neueren Tokens des Kontos ungültig (Abmeldung auf allen Geräten).
     */
    @Test
    void reusedRefreshTokenRevokesAllSessions() throws Exception {
        String email = randomEmail();
        registerVerifiedUser(email, "geheim123");
        String stolen = refreshCookie(login(email, "geheim123"));
        String current = refreshCookie(refresh(stolen).andExpect(status().isOk()));

        refresh(stolen).andExpect(status().isUnauthorized());
        refresh(current).andExpect(status().isUnauthorized());
    }

    /** Ohne Cookie oder mit ausgedachtem Token → 401. */
    @Test
    void refreshWithoutValidCookieFails() throws Exception {
        refresh(null).andExpect(status().isUnauthorized());
        refresh("ausgedacht").andExpect(status().isUnauthorized());
    }

    /**
     * Abmelden widerruft das Refresh-Token und löscht das Cookie (Max-Age=0). Wird das abgemeldete Token
     * später noch einmal benutzt, ist das kein Diebstahl: Andere Geräte bleiben angemeldet.
     */
    @Test
    void logoutRevokesOnlyThisDevice() throws Exception {
        String email = randomEmail();
        registerVerifiedUser(email, "geheim123");
        String token = refreshCookie(login(email, "geheim123"));
        String otherDevice = refreshCookie(login(email, "geheim123"));

        ResultActions logout = mockMvc.perform(post("/api/auth/logout").cookie(new Cookie("refresh_token", token)))
                .andExpect(status().isNoContent());
        assertThat(logout.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");

        refresh(token).andExpect(status().isUnauthorized());
        refresh(otherDevice).andExpect(status().isOk());
    }

    /** Passwort ändern meldet alle anderen Geräte ab; dieses Gerät bekommt ein neues, gültiges Cookie. */
    @Test
    void changingPasswordLogsOutOtherDevices() throws Exception {
        String email = randomEmail();
        registerVerifiedUser(email, "altesPasswort1");
        ResultActions laptop = login(email, "altesPasswort1");
        String phoneCookie = refreshCookie(login(email, "altesPasswort1"));

        ResultActions changed = putJson("/api/account/password", accessToken(laptop), """
                {"currentPassword": "altesPasswort1", "newPassword": "neuesPasswort1"}
                """).andExpect(status().isNoContent());
        String newLaptopCookie = refreshCookie(changed);

        refresh(phoneCookie).andExpect(status().isUnauthorized());
        refresh(refreshCookie(laptop)).andExpect(status().isUnauthorized());
        refresh(newLaptopCookie).andExpect(status().isOk());
    }

    /** Passwort zurücksetzen ("Passwort vergessen") meldet alle Geräte ab. */
    @Test
    void resettingPasswordLogsOutEverywhere() throws Exception {
        String email = randomEmail();
        registerVerifiedUser(email, "altesPasswort1");
        String cookie = refreshCookie(login(email, "altesPasswort1"));

        mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\"}".formatted(email))).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"%s\", \"password\": \"neuesPasswort1\"}".formatted(tokenFromLastMailTo(email))))
                .andExpect(status().isOk());

        refresh(cookie).andExpect(status().isUnauthorized());
    }
}
