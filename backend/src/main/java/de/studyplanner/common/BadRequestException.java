package de.studyplanner.common;

/**
 * Fachlich ungültige Anfrage, z. B. Endzeit vor Startzeit.
 * Wird im GlobalExceptionHandler in eine Antwort mit HTTP 400 umgewandelt.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
