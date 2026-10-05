package de.studyplanner.auth;

import jakarta.validation.constraints.NotBlank;

/** JSON-Body für POST /api/auth/login. Die Annotationen werden durch @Valid im Controller geprüft. */
public record LoginRequest(
        @NotBlank(message = "E-Mail ist erforderlich")
        String email,

        @NotBlank(message = "Passwort ist erforderlich")
        String password
) {
}
