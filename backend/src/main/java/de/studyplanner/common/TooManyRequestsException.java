package de.studyplanner.common;

/**
 * Zu viele Versuche in kurzer Zeit, z. B. falsche Passwörter beim Login.
 * Wird im GlobalExceptionHandler in eine Antwort mit HTTP 429 umgewandelt.
 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
