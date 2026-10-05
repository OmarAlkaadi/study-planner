package de.studyplanner.auth;

/**
 * Ergebnis einer erfolgreichen Anmeldung oder Erneuerung innerhalb des Backends:
 * body geht als JSON an das Frontend (Access-Token + Benutzer), refreshToken kommt ins httpOnly-Cookie
 * und taucht im JSON nie auf.
 */
public record AuthSession(AuthResponse body, String refreshToken) {
}
