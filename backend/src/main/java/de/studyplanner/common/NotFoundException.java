package de.studyplanner.common;

/**
 * Wird geworfen, wenn eine Ressource nicht existiert oder nicht dem angemeldeten Benutzer gehört.
 * Fremde Daten liefern also bewusst 404 statt 403 – so verrät die API nicht, dass es sie gibt.
 * Der GlobalExceptionHandler macht daraus eine Antwort mit HTTP 404.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
