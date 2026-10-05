package de.studyplanner.common;

/**
 * Login mit richtigem Passwort, aber die E-Mail-Adresse ist noch nicht bestätigt.
 * Wird im GlobalExceptionHandler in HTTP 403 mit dem Zusatzfeld "code": "EMAIL_NOT_VERIFIED" umgewandelt,
 * damit das Frontend den Button "Bestätigungsmail erneut senden" anzeigen kann.
 */
public class EmailNotVerifiedException extends RuntimeException {

    public static final String CODE = "EMAIL_NOT_VERIFIED";

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
