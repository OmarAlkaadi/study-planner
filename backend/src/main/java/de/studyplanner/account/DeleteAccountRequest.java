package de.studyplanner.account;

import jakarta.validation.constraints.NotBlank;

/** JSON-Body für DELETE /api/account: das Passwort als Bestätigung, damit niemand versehentlich löscht. */
public record DeleteAccountRequest(
        @NotBlank(message = "Passwort ist erforderlich")
        String password
) {
}
