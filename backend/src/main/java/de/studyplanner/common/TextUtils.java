package de.studyplanner.common;

/** Kleine Hilfsfunktionen für Texteingaben. */
public final class TextUtils {

    private TextUtils() {
    }

    /** Entfernt Leerzeichen am Rand und macht aus leeren Texten {@code null}. */
    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Baut ein LIKE-Muster für eine Suche ohne Beachtung von Groß-/Kleinschreibung.
     * Die Sonderzeichen %, _ und \ werden maskiert, damit sie wörtlich gesucht werden.
     */
    public static String likePattern(String query) {
        String escaped = query.trim().toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
