package de.studyplanner.auth;

import de.studyplanner.common.MessageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Controller für Registrierung, Login, Erneuern/Abmelden, E-Mail-Bestätigung, "Passwort vergessen"
 * und den aktuell angemeldeten Benutzer (Basis-Pfad /api/auth).
 *
 * Der Controller nimmt nur die HTTP-Anfrage entgegen und gibt sie an den AuthService weiter.
 * Login und Erneuern setzen zusätzlich das Refresh-Token als httpOnly-Cookie (siehe AuthCookies);
 * im JSON steht nur das kurzlebige Access-Token.
 * @Valid prüft den JSON-Body anhand der Annotationen im Request-DTO; bei Fehlern antwortet der
 * GlobalExceptionHandler mit HTTP 400. Alle POST-Endpunkte hier sind öffentlich (siehe SecurityConfig),
 * nur /me braucht ein gültiges JWT.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookies authCookies;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, AuthCookies authCookies, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.authCookies = authCookies;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * POST /api/auth/register – legt ein Konto an und schickt die Bestätigungsmail (HTTP 201).
     * Es gibt noch kein Token: Anmelden kann man sich erst nach der Bestätigung.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * POST /api/auth/login – prüft E-Mail und Passwort; liefert ein Access-Token (JSON) und setzt das
     * Refresh-Token-Cookie. Fehler: 401 (falsche Daten), 403 (E-Mail nicht bestätigt), 429 (zu viele Fehlversuche).
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return withRefreshCookie(authService.login(request));
    }

    /**
     * POST /api/auth/refresh – erneuert die Anmeldung mit dem Refresh-Token aus dem Cookie.
     * Liefert ein neues Access-Token und ersetzt das Cookie durch ein neues Refresh-Token (Rotation).
     * Das Frontend ruft das beim Start der App und immer dann auf, wenn das Access-Token abgelaufen ist.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = AuthCookies.REFRESH_COOKIE, required = false) String refreshToken) {
        return withRefreshCookie(authService.refresh(refreshToken));
    }

    /** POST /api/auth/logout – meldet dieses Gerät ab: Refresh-Token widerrufen, Cookie löschen (HTTP 204). */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AuthCookies.REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookies.clearRefreshCookie().toString())
                .build();
    }

    /**
     * POST /api/auth/verify-email – löst den Link aus der Bestätigungsmail ein und aktiviert das Konto.
     * Meldet bewusst nicht an: Das Frontend leitet danach zur Login-Seite mit einem Hinweis weiter.
     */
    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody TokenRequest request) {
        return authService.verifyEmail(request.token());
    }

    /** POST /api/auth/resend-verification – schickt die Bestätigungsmail erneut (Antwort immer gleich). */
    @PostMapping("/resend-verification")
    public MessageResponse resendVerification(@Valid @RequestBody EmailRequest request) {
        return authService.resendVerification(request.email());
    }

    /** POST /api/auth/forgot-password – schickt einen Link zum Zurücksetzen des Passworts (Antwort immer gleich). */
    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody EmailRequest request) {
        return authService.forgotPassword(request.email());
    }

    /** POST /api/auth/reset-password – setzt mit dem Token aus der Mail ein neues Passwort. */
    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    /**
     * GET /api/auth/me – liefert den angemeldeten Benutzer.
     * Das Frontend ruft das beim Start auf, um zu prüfen, ob das gespeicherte Token noch gültig ist.
     */
    @GetMapping("/me")
    public UserResponse me() {
        return authService.me();
    }

    /** Antwort mit dem Access-Token im JSON und dem Refresh-Token im httpOnly-Cookie. */
    private ResponseEntity<AuthResponse> withRefreshCookie(AuthSession session) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        authCookies.refreshCookie(session.refreshToken(), refreshTokenService.validity()).toString())
                .body(session.body());
    }
}
