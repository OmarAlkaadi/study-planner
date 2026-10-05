package de.studyplanner.auth;

import de.studyplanner.common.BadRequestException;
import de.studyplanner.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Erstellt und prüft die Einmal-Tokens für die Links in den E-Mails.
 *
 * Ablauf: create() erzeugt ein zufälliges Token (256 Bit, siehe SecureTokens), speichert nur dessen SHA-256-Hash und gibt das
 * Token selbst zurück – es landet ausschließlich im Link der E-Mail. consume() bildet beim Klick auf den
 * Link wieder den Hash, sucht ihn in der Datenbank und markiert das Token als benutzt.
 */
@Service
@Transactional
public class UserTokenService {

    private final UserTokenRepository userTokenRepository;

    public UserTokenService(UserTokenRepository userTokenRepository) {
        this.userTokenRepository = userTokenRepository;
    }

    /**
     * Erzeugt ein neues Token für den Benutzer. Ältere Tokens derselben Art werden gelöscht,
     * damit immer nur der Link aus der neuesten E-Mail funktioniert.
     *
     * @return das Token im Klartext (URL-sicher) – nur für den Link in der E-Mail bestimmt
     */
    public String create(User user, TokenPurpose purpose, Duration validity) {
        userTokenRepository.deleteByUserIdAndPurpose(user.getId(), purpose);
        String token = SecureTokens.generate();
        userTokenRepository.save(new UserToken(user, SecureTokens.hash(token), purpose, LocalDateTime.now().plus(validity)));
        return token;
    }

    /**
     * Löst ein Token ein und gibt den zugehörigen Benutzer zurück.
     *
     * @throws BadRequestException wenn das Token unbekannt, schon benutzt oder abgelaufen ist (HTTP 400)
     */
    public User consume(String token, TokenPurpose purpose) {
        UserToken userToken = userTokenRepository.findByTokenHashAndPurpose(SecureTokens.hash(token), purpose)
                .filter(t -> t.isUsable(LocalDateTime.now()))
                .orElseThrow(() -> new BadRequestException(purpose == TokenPurpose.EMAIL_VERIFICATION
                        ? "Der Bestätigungslink ist ungültig oder abgelaufen. Fordere beim Login einen neuen an."
                        : "Der Link zum Zurücksetzen ist ungültig oder abgelaufen. Fordere einen neuen an."));
        userToken.markUsed();
        return userToken.getUser();
    }

    /** true, wenn innerhalb des Zeitraums schon ein Token dieser Art erstellt wurde (verhindert Mail-Spam). */
    @Transactional(readOnly = true)
    public boolean createdWithin(User user, TokenPurpose purpose, Duration period) {
        return userTokenRepository.existsByUserIdAndPurposeAndCreatedAtAfter(
                user.getId(), purpose, LocalDateTime.now().minus(period));
    }
}
