package de.studyplanner;

import de.studyplanner.auth.AccountCleanupJob;
import de.studyplanner.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Nächtliches Aufräumen: unbestätigte Konten verschwinden nach 7 Tagen, bestätigte bleiben. */
class AccountCleanupIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AccountCleanupJob cleanupJob;

    @Autowired
    private UserRepository userRepository;

    /** Nach 6 Tagen bleibt das unbestätigte Konto noch, nach 8 Tagen ist es weg und die Adresse wieder frei. */
    @Test
    void removesUnverifiedAccountsAfterSevenDays() throws Exception {
        String unverified = "aufraeumen-" + UUID.randomUUID() + "@test.de";
        String verified = "bleibt-" + UUID.randomUUID() + "@test.de";
        register(unverified, "geheim123", "Max").andExpect(status().isCreated());
        registerVerifiedUser(verified, "geheim123");

        cleanupJob.cleanUp(LocalDateTime.now().plusDays(6));
        assertThat(userRepository.findByEmail(unverified)).isPresent();

        cleanupJob.cleanUp(LocalDateTime.now().plusDays(8));
        assertThat(userRepository.findByEmail(unverified)).isEmpty();
        assertThat(userRepository.findByEmail(verified)).isPresent();
        login(verified, "geheim123").andExpect(status().isOk());

        // Die Adresse ist wieder frei und kann neu registriert werden
        register(unverified, "geheim123", "Max").andExpect(status().isCreated());
        assertThat(userRepository.findByEmail(unverified)).isPresent();
    }
}
