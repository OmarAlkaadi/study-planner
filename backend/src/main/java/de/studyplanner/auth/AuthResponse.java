package de.studyplanner.auth;

/**
 * Antwort auf Login und Registrierung.
 * Das Frontend speichert das Token im localStorage und schickt es bei jeder weiteren Anfrage
 * im Header "Authorization: Bearer &lt;token&gt;" mit.
 */
public record AuthResponse(String token, UserResponse user) {
}
