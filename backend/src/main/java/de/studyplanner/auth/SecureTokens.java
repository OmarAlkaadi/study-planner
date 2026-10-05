package de.studyplanner.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Gemeinsame Hilfsfunktionen für alle Zufalls-Tokens der App (Links in E-Mails, Refresh-Tokens).
 *
 * Tokens werden mit einem kryptografisch sicheren Zufallsgenerator erzeugt (256 Bit). In der Datenbank
 * wird nie das Token selbst gespeichert, sondern nur sein SHA-256-Hash – wer die Datenbank lesen kann,
 * kann damit also nichts anfangen.
 */
public final class SecureTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SecureTokens() {
    }

    /** Neues Zufalls-Token (32 Byte), URL-sicher kodiert – passt in Links und Cookies. */
    public static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 als Hex-Text (64 Zeichen) – so wird ein Token in der Datenbank gespeichert und gesucht. */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 ist in jeder Java-Version vorhanden", e);
        }
    }
}
