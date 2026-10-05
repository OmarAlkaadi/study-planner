package de.studyplanner.common;

/**
 * Aktion ist für diesen Benutzer nicht erlaubt, z. B. die Kontodaten des Demo-Accounts zu ändern.
 * Wird im GlobalExceptionHandler in eine Antwort mit HTTP 403 umgewandelt.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
