package de.studyplanner.account;

import de.studyplanner.auth.AuthCookies;
import de.studyplanner.auth.RefreshTokenService;
import de.studyplanner.auth.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Controller für das eigene Konto (Basis-Pfad /api/account). Alle Endpunkte brauchen ein JWT.
 * Den aktuellen Benutzer liefert weiterhin GET /api/auth/me.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;
    private final AuthCookies authCookies;
    private final RefreshTokenService refreshTokenService;

    public AccountController(AccountService accountService, AuthCookies authCookies,
                             RefreshTokenService refreshTokenService) {
        this.accountService = accountService;
        this.authCookies = authCookies;
        this.refreshTokenService = refreshTokenService;
    }

    /** PUT /api/account/profile – ändert den Anzeigenamen und liefert den aktualisierten Benutzer. */
    @PutMapping("/profile")
    public UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return accountService.updateProfile(request);
    }

    /**
     * PUT /api/account/password – ändert das Passwort (HTTP 204, bei falschem aktuellem Passwort 400).
     * Alle anderen Geräte werden abgemeldet; dieses Gerät bekommt ein neues Refresh-Cookie.
     */
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String refreshToken = accountService.changePassword(request);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE,
                        authCookies.refreshCookie(refreshToken, refreshTokenService.validity()).toString())
                .build();
    }

    /**
     * DELETE /api/account – löscht das Konto mit allen Daten (HTTP 204) und das Refresh-Cookie.
     * Das Passwort kommt im Body.
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        accountService.deleteAccount(request);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookies.clearRefreshCookie().toString())
                .build();
    }
}
