package de.studyplanner.auth;

import jakarta.validation.constraints.NotBlank;

/** JSON-Body für POST /api/auth/verify-email: das Token aus dem Link der Bestätigungsmail. */
public record TokenRequest(
        @NotBlank(message = "Token fehlt")
        String token
) {
}
