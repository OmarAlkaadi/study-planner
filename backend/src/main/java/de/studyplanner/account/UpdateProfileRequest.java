package de.studyplanner.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** JSON-Body für PUT /api/account/profile: der neue Anzeigename. */
public record UpdateProfileRequest(
        @NotBlank(message = "Name ist erforderlich")
        @Size(max = 100, message = "Name darf höchstens 100 Zeichen lang sein")
        String displayName
) {
}
