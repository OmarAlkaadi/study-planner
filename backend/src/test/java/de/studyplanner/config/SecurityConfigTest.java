package de.studyplanner.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit-Tests für die Prüfung des JWT-Geheimnisses (ohne Spring, ohne Datenbank). */
class SecurityConfigTest {

    /** Ohne Geheimnis wird ein zufälliger Schlüssel erzeugt – bei jedem Aufruf ein anderer. */
    @Test
    void missingSecretGivesRandomKey() {
        byte[] first = SecurityConfig.resolveSecret("");
        byte[] second = SecurityConfig.resolveSecret(null);
        assertThat(first).hasSize(48);
        assertThat(first).isNotEqualTo(second);
    }

    /** Öffentlich bekannte Platzhalter werden abgelehnt – damit ließen sich Tokens fälschen. */
    @Test
    void knownPlaceholdersAreRejected() {
        SecurityConfig.KNOWN_PLACEHOLDERS.forEach(placeholder ->
                assertThatThrownBy(() -> SecurityConfig.resolveSecret(placeholder))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Platzhalter"));
    }

    /** Zu kurze Geheimnisse werden abgelehnt, ausreichend lange übernommen. */
    @Test
    void secretMustHaveAtLeast32Bytes() {
        assertThatThrownBy(() -> SecurityConfig.resolveSecret("zu-kurz"))
                .hasMessageContaining("32 Zeichen");
        assertThat(SecurityConfig.resolveSecret("ein-eigener-geheimer-schluessel-mit-genug-zeichen")).hasSize(49);
    }
}
