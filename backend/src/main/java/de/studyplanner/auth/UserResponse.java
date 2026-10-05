package de.studyplanner.auth;

import de.studyplanner.demo.DemoAccount;
import de.studyplanner.user.User;

/**
 * Öffentliche Benutzerdaten für das Frontend – bewusst ohne Passwort-Hash.
 * demo = true beim öffentlichen Demo-Account: Das Frontend sperrt dann die Kontoformulare.
 */
public record UserResponse(Long id, String email, String displayName, boolean demo) {

    /** Wandelt die User-Entity in dieses DTO um. */
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), DemoAccount.isDemo(user.getEmail()));
    }
}
