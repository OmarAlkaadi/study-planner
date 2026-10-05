package de.studyplanner.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON-Body für POST /api/auth/register.
 * Die Validierungsregeln stehen direkt an den Feldern; bei Verstößen landet die jeweilige Meldung
 * im "errors"-Objekt der Fehlerantwort und wird im Frontend unter dem passenden Eingabefeld angezeigt.
 */
public record RegisterRequest(
        @NotBlank(message = "E-Mail ist erforderlich")
        @Email(message = "Keine gültige E-Mail-Adresse")
        @Size(max = 255, message = "E-Mail ist zu lang")
        String email,

        @NotBlank(message = "Passwort ist erforderlich")
        @Size(min = 8, max = 72, message = "Passwort muss 8 bis 72 Zeichen lang sein")
        String password,

        @NotBlank(message = "Name ist erforderlich")
        @Size(max = 100, message = "Name darf höchstens 100 Zeichen lang sein")
        String displayName
) {
}
