package de.studyplanner.common;

/**
 * Anmeldung nicht möglich: falsche Login-Daten (E-Mail unbekannt oder Passwort falsch) oder ein
 * ungültiges/abgelaufenes Refresh-Token. Wird im GlobalExceptionHandler in eine Antwort mit HTTP 401
 * umgewandelt – mit optionalem Zusatzfeld "code" für Sonderfälle, auf die das Frontend reagiert.
 */
public class InvalidCredentialsException extends RuntimeException {

    private final String code;

    public InvalidCredentialsException(String message) {
        this(message, null);
    }

    public InvalidCredentialsException(String message, String code) {
        super(message);
        this.code = code;
    }

    /** Fehlercode für das Frontend (z. B. "REFRESH_TOKEN_ROTATED") oder null. */
    public String getCode() {
        return code;
    }
}
