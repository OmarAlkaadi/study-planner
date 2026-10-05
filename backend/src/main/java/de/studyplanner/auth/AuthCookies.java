package de.studyplanner.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Baut das Cookie, in dem das Refresh-Token zum Browser geht.
 *
 * - HttpOnly: JavaScript kann das Cookie nicht lesen – auch eingeschleuster Schadcode (XSS) kommt nicht heran
 * - SameSite=Strict: Der Browser schickt es nur bei Anfragen von der eigenen Seite mit (Schutz vor CSRF)
 * - Path=/api: Es geht nur an das Backend, nicht an statische Dateien
 * - Secure: nur über HTTPS – lokal (http://localhost) aus, auf einem echten Server mit HTTPS über
 *   AUTH_COOKIE_SECURE=true einschalten
 */
@Component
public class AuthCookies {

    /** Name des Cookies – das Backend liest es in AuthController (refresh, logout). */
    public static final String REFRESH_COOKIE = "refresh_token";

    private static final String PATH = "/api";

    private final boolean secure;

    public AuthCookies(@Value("${app.auth.cookie-secure}") boolean secure) {
        this.secure = secure;
    }

    /** Cookie mit neuem Refresh-Token, gültig für die angegebene Dauer. */
    public ResponseCookie refreshCookie(String token, Duration maxAge) {
        return base(token).maxAge(maxAge).build();
    }

    /** Leeres Cookie mit Lebensdauer 0 – der Browser löscht daraufhin das Refresh-Token (Abmelden). */
    public ResponseCookie clearRefreshCookie() {
        return base("").maxAge(0).build();
    }

    /** Gemeinsame Eigenschaften beider Cookies. */
    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH);
    }
}
