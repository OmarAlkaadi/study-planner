package de.studyplanner.auth;

import de.studyplanner.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Nächtliches Aufräumen (app.cleanup.cron, Standard 3:30 Uhr):
 * - Konten, deren E-Mail-Adresse nach app.cleanup.unverified-account-days Tagen (Standard 7) noch nicht
 *   bestätigt ist, werden gelöscht. Sonst könnte jemand eine fremde Adresse registrieren und dauerhaft "belegen".
 * - Abgelaufene Einmal-Tokens aus E-Mail-Links werden gelöscht.
 * - Abgelaufene und seit über einem Tag abgemeldete Refresh-Tokens werden gelöscht
 *   (durch Erneuern ersetzte bleiben bis zum Ablauf – für die Erkennung gestohlener Tokens).
 */
@Component
public class AccountCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(AccountCleanupJob.class);

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final int unverifiedAccountDays;

    public AccountCleanupJob(UserRepository userRepository, UserTokenRepository userTokenRepository,
                             RefreshTokenRepository refreshTokenRepository,
                             @Value("${app.cleanup.unverified-account-days:7}") int unverifiedAccountDays) {
        this.userRepository = userRepository;
        this.userTokenRepository = userTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.unverifiedAccountDays = unverifiedAccountDays;
    }

    /** Wird von Spring nach Zeitplan aufgerufen. */
    @Scheduled(cron = "${app.cleanup.cron:0 30 3 * * *}")
    public void runScheduled() {
        cleanUp(LocalDateTime.now());
    }

    /**
     * Das eigentliche Aufräumen mit einem frei wählbaren "Jetzt" – so lässt es sich im Test prüfen,
     * ohne eine Woche zu warten.
     */
    @Transactional
    public void cleanUp(LocalDateTime now) {
        int accounts = userRepository.deleteUnverifiedCreatedBefore(now.minusDays(unverifiedAccountDays));
        int mailTokens = userTokenRepository.deleteExpired(now);
        int refreshTokens = refreshTokenRepository.deleteStaleForAll(now, now.minusDays(1));
        log.info("Aufgeräumt: {} unbestätigte Konten, {} abgelaufene Mail-Tokens, {} alte Refresh-Tokens",
                accounts, mailTokens, refreshTokens);
    }
}
