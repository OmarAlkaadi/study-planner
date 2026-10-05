package de.studyplanner.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** JSON-Body für POST /api/auth/reset-password: Token aus dem Mail-Link und das neue Passwort. */
public record ResetPasswordRequest(
        @NotBlank(message = "Token fehlt")
        String token,

        @NotBlank(message = "Passwort ist erforderlich")
        @Size(min = 8, max = 72, message = "Passwort muss 8 bis 72 Zeichen lang sein")
        String password
) {
}
