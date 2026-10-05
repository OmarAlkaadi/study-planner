package de.studyplanner.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** JSON-Body für PUT /api/account/password: aktuelles Passwort (zur Sicherheit) und neues Passwort. */
public record ChangePasswordRequest(
        @NotBlank(message = "Aktuelles Passwort ist erforderlich")
        String currentPassword,

        @NotBlank(message = "Neues Passwort ist erforderlich")
        @Size(min = 8, max = 72, message = "Passwort muss 8 bis 72 Zeichen lang sein")
        String newPassword
) {
}
