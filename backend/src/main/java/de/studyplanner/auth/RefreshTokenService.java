package de.studyplanner.auth;

import de.studyplanner.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Verwaltet die Refresh-Tokens (länger angemeldet bleiben).
 *
 * - issue(): neues Refresh-Token nach dem Login (Standard 30 Tage gültig, app.auth.refresh-token-days)
 * - rotate(): beim Erneuern wird das alte Token widerrufen und ein neues ausgegeben (Rotation).
 *   Taucht ein durch Erneuern ersetztes Token später noch einmal auf, wurde es vermutlich gestohlen:
 *   Dann werden ALLE Tokens des Benutzers widerrufen (Abmeldung auf allen Geräten).
 *   Ausnahme: kurz nach dem Ersetzen (app.auth.refresh-reuse-grace-seconds, Standard 60 s) – das passiert
 *   harmlos, wenn zwei Browser-Tabs gleichzeitig erneuern. Der zweite Tab bekommt dann nur eine Ablehnung
 *   und versucht es mit dem inzwischen neuen Cookie noch einmal.
 * - revoke() / revokeAll(): Abmelden auf diesem bzw. allen Geräten
 */
@Service
@Transactional
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    /** Widerrufene Tokens werden nach dieser Zeit beim nächsten Login aufgeräumt. */
    private static final Duration KEEP_REVOKED = Duration.ofDays(1);

    /** Ergebnis von rotate(): neues Token oder der Grund, warum nicht erneuert wurde. */
    public sealed interface Rotation {
        /** Erfolgreich erneuert: Benutzer und neues Refresh-Token (Klartext, nur für das Cookie). */
        record Rotated(User user, String newToken) implements Rotation {
        }

        /** Token gerade erst ersetzt (anderer Tab war schneller) – Client soll es mit dem neuen Cookie erneut versuchen. */
        record RecentlyRotated() implements Rotation {
        }

        /** Unbekannt, abgelaufen, abgemeldet oder wiederverwendet – neu anmelden. */
        record Invalid() implements Rotation {
        }
    }

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration validity;
    private final Duration reuseGrace;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${app.auth.refresh-token-days}") long refreshTokenDays,
                               @Value("${app.auth.refresh-reuse-grace-seconds}") long reuseGraceSeconds) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.validity = Duration.ofDays(refreshTokenDays);
        this.reuseGrace = Duration.ofSeconds(reuseGraceSeconds);
    }

    /** Gültigkeit eines Refresh-Tokens – auch die Lebensdauer des Cookies. */
    public Duration validity() {
        return validity;
    }

    /** Erstellt ein neues Refresh-Token für den Benutzer und räumt dabei alte Einträge auf. */
    public String issue(User user) {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteStale(user.getId(), now, now.minus(KEEP_REVOKED));
        String token = SecureTokens.generate();
        refreshTokenRepository.save(new RefreshToken(user, SecureTokens.hash(token), now.plus(validity)));
        return token;
    }

    /**
     * Erneuert ein Refresh-Token (alt widerrufen, neu ausgeben). Gibt bewusst ein Ergebnis zurück statt
     * eine Exception zu werfen: Sonst würde die Transaktion zurückgerollt – auch das Widerrufen aller
     * Tokens bei erkannter Wiederverwendung. (Der Aufrufer AuthService.refresh wirft danach zwar HTTP 401,
     * ist aber mit noRollbackFor so eingestellt, dass das Widerrufen gespeichert bleibt.)
     */
    public Rotation rotate(String token) {
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash(SecureTokens.hash(token));
        if (found.isEmpty()) {
            return new Rotation.Invalid();
        }
        RefreshToken refreshToken = found.get();
        LocalDateTime now = LocalDateTime.now();

        if (refreshToken.getReplacedAt() != null) {
            // Durch Erneuern ersetztes Token taucht wieder auf: kurz danach harmlos (zweiter Tab), sonst Diebstahl
            if (refreshToken.getReplacedAt().plus(reuseGrace).isAfter(now)) {
                return new Rotation.RecentlyRotated();
            }
            log.warn("Bereits ersetztes Refresh-Token wiederverwendet (Benutzer {}) – alle Sitzungen werden beendet",
                    refreshToken.getUser().getId());
            refreshTokenRepository.revokeAllForUser(refreshToken.getUser().getId(), now);
            return new Rotation.Invalid();
        }
        if (refreshToken.getRevokedAt() != null) {
            // Abgemeldet oder nach Passwortänderung widerrufen – einfach ungültig, kein Hinweis auf Diebstahl
            return new Rotation.Invalid();
        }
        if (!refreshToken.getExpiresAt().isAfter(now)) {
            return new Rotation.Invalid();
        }

        refreshToken.markReplaced();
        User user = refreshToken.getUser();
        return new Rotation.Rotated(user, issue(user));
    }

    /** Widerruft ein einzelnes Token (Abmelden auf diesem Gerät). Unbekannte Tokens werden ignoriert. */
    public void revoke(String token) {
        refreshTokenRepository.findByTokenHash(SecureTokens.hash(token)).ifPresent(RefreshToken::revoke);
    }

    /** Widerruft alle Tokens des Benutzers (Abmelden auf allen Geräten, z. B. nach Passwortänderung). */
    public void revokeAll(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId, LocalDateTime.now());
    }
}
