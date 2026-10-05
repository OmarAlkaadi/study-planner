package de.studyplanner.demo;

import de.studyplanner.common.ForbiddenException;

/**
 * Zugangsdaten und Schutzregeln des öffentlichen Demo-Accounts.
 *
 * Weil das Passwort öffentlich in der README steht, darf niemand die Kontodaten ändern: Name, Passwort und
 * das Konto selbst sind gesperrt, "Passwort vergessen" verschickt keine Mail, und Fehlversuche beim Login
 * führen zu keiner Sperre (sonst könnte jeder die Demo für alle anderen blockieren). Kurse, Klausuren usw.
 * dürfen Besucher ausprobieren – sie werden jede Nacht zurückgesetzt (DemoDataInitializer).
 */
public final class DemoAccount {

    public static final String EMAIL = "demo@studyplanner.de";
    public static final String PASSWORD = "demo1234";

    private DemoAccount() {
    }

    /** true, wenn die (normalisierte oder nicht normalisierte) Adresse die des Demo-Accounts ist. */
    public static boolean isDemo(String email) {
        return email != null && EMAIL.equalsIgnoreCase(email.trim());
    }

    /** Wirft ForbiddenException (HTTP 403), wenn es sich um den Demo-Account handelt. */
    public static void requireNotDemo(String email) {
        if (isDemo(email)) {
            throw new ForbiddenException("Die Kontodaten des Demo-Accounts sind gesperrt. "
                    + "Registriere dich für ein eigenes Konto, um sie ändern zu können.");
        }
    }
}
