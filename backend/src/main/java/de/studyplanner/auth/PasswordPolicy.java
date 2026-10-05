package de.studyplanner.auth;

import de.studyplanner.common.BadRequestException;

import java.nio.charset.StandardCharsets;

/**
 * Gemeinsame Passwort-Regel für Registrierung, Passwort-Reset und Passwort-Änderung.
 * Die Mindestlänge (8 Zeichen) prüfen die Request-DTOs per @Size; hier geht es um die Obergrenze in Bytes,
 * die mit Annotationen nicht prüfbar ist.
 */
public final class PasswordPolicy {

    /** BCrypt verarbeitet maximal 72 Bytes. */
    public static final int MAX_PASSWORD_BYTES = 72;

    private PasswordPolicy() {
    }

    /** true, wenn das Passwort mehr Bytes hat, als BCrypt verarbeiten kann (Umlaute/Emojis zählen mehrfach). */
    public static boolean tooLong(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
    }

    /** Wirft BadRequestException (HTTP 400), wenn das Passwort zu lang ist. */
    public static void requireValidLength(String password) {
        if (tooLong(password)) {
            throw new BadRequestException("Das Passwort ist zu lang.");
        }
    }
}
