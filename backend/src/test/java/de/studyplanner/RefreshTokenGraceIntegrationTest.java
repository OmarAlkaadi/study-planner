package de.studyplanner;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kulanzzeit für parallele Browser-Tabs: Erneuern zwei Tabs gleichzeitig, benutzt der langsamere ein Token,
 * das der schnellere gerade ersetzt hat. Das darf NICHT als Diebstahl gelten – der langsamere Tab bekommt
 * nur 401 mit dem Code REFRESH_TOKEN_ROTATED und versucht es mit dem neuen Cookie noch einmal.
 * (Eigene Testklasse, weil hier eine andere Einstellung als in den übrigen Tests gilt.)
 */
@TestPropertySource(properties = "app.auth.refresh-reuse-grace-seconds=60")
class RefreshTokenGraceIntegrationTest extends IntegrationTestBase {

    /** Wiederverwendung direkt nach dem Ersetzen → 401 mit Code, aber das neue Token bleibt gültig. */
    @Test
    void recentlyRotatedTokenDoesNotRevokeSession() throws Exception {
        String email = "tabs-" + UUID.randomUUID() + "@test.de";
        registerVerifiedUser(email, "geheim123");
        String old = refreshCookie(login(email, "geheim123"));
        String current = refreshCookie(refresh(old).andExpect(status().isOk()));

        refresh(old)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_ROTATED"));
        refresh(current).andExpect(status().isOk());
    }
}
