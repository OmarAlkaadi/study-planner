package de.studyplanner.auth;

/** Wofür ein Einmal-Token aus einer E-Mail gedacht ist. */
public enum TokenPurpose {
    /** Link in der Bestätigungsmail nach der Registrierung (24 Stunden gültig). */
    EMAIL_VERIFICATION,
    /** Link in der Mail "Passwort zurücksetzen" (60 Minuten gültig). */
    PASSWORD_RESET
}
