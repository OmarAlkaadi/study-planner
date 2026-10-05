package de.studyplanner.auth;

import de.studyplanner.common.TooManyRequestsException;
import de.studyplanner.demo.DemoAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Schutz gegen Passwort-Raten (Brute Force) beim Login.
 *
 * Nach {@code app.login.max-attempts} Fehlversuchen (Standard 5) für dieselbe E-Mail-Adresse wird der Login
 * für {@code app.login.lock-minutes} Minuten (Standard 5) gesperrt → HTTP 429.
 * Gezählt wird pro E-Mail-Adresse, auch für Adressen ohne Konto – so verrät die Sperre nicht,
 * welche Adressen registriert sind.
 *
 * Ausnahme: der Demo-Account – sein Passwort ist öffentlich, eine Sperre würde nur die Demo für alle blockieren.
 * Zusätzlich begrenzt nginx die Anzahl der Login-Versuche pro IP-Adresse (siehe frontend/nginx.conf).
 *
 * Die Zähler liegen nur im Arbeitsspeicher: Nach einem Neustart des Backends sind sie zurückgesetzt.
 * Für eine einzelne Backend-Instanz (wie hier mit Docker Compose) reicht das völlig.
 */
@Component
public class LoginAttemptService {

    /** Ab so vielen gespeicherten Einträgen werden abgelaufene aufgeräumt (Schutz vor vollem Speicher). */
    private static final int CLEANUP_THRESHOLD = 10_000;

    /** Stand für eine E-Mail-Adresse: Anzahl Fehlversuche bzw. gesperrt bis (null = nicht gesperrt). */
    private record Attempts(int failures, Instant lockedUntil) {
    }

    private final int maxAttempts;
    private final Duration lockDuration;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(@Value("${app.login.max-attempts:5}") int maxAttempts,
                               @Value("${app.login.lock-minutes:5}") long lockMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    /**
     * Wirft TooManyRequestsException, solange die E-Mail-Adresse gesperrt ist.
     * Wird vor der Passwortprüfung aufgerufen – während der Sperre wird also gar nicht erst geprüft.
     */
    public void checkNotLocked(String email) {
        if (DemoAccount.isDemo(email)) {
            return;
        }
        Attempts current = attempts.get(email);
        if (current == null || current.lockedUntil() == null) {
            return;
        }
        Instant now = Instant.now();
        if (now.isBefore(current.lockedUntil())) {
            long minutes = Math.max(1, (Duration.between(now, current.lockedUntil()).toSeconds() + 59) / 60);
            throw new TooManyRequestsException("Zu viele fehlgeschlagene Anmeldeversuche. Bitte versuche es in "
                    + minutes + (minutes == 1 ? " Minute" : " Minuten") + " erneut.");
        }
        attempts.remove(email);
    }

    /** Zählt einen Fehlversuch; beim Erreichen des Limits wird die Adresse gesperrt. */
    public void loginFailed(String email) {
        if (DemoAccount.isDemo(email)) {
            return;
        }
        if (attempts.size() > CLEANUP_THRESHOLD) {
            Instant now = Instant.now();
            attempts.values().removeIf(a -> a.lockedUntil() == null || a.lockedUntil().isBefore(now));
        }
        attempts.compute(email, (key, current) -> {
            int failures = (current == null || current.lockedUntil() != null ? 0 : current.failures()) + 1;
            return failures >= maxAttempts
                    ? new Attempts(0, Instant.now().plus(lockDuration))
                    : new Attempts(failures, null);
        });
    }

    /** Erfolgreicher Login oder Passwort-Reset: Zähler und Sperre zurücksetzen. */
    public void reset(String email) {
        attempts.remove(email);
    }
}
