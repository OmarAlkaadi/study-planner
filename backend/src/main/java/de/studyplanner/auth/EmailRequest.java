package de.studyplanner.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** JSON-Body mit nur einer E-Mail-Adresse – für "Passwort vergessen" und "Bestätigungsmail erneut senden". */
public record EmailRequest(
        @NotBlank(message = "E-Mail ist erforderlich")
        @Email(message = "Keine gültige E-Mail-Adresse")
        String email
) {
}
