package de.studyplanner.auth;

import de.studyplanner.common.CurrentUser;
import de.studyplanner.common.EmailNotVerifiedException;
import de.studyplanner.common.InvalidCredentialsException;
import de.studyplanner.common.MessageResponse;
import de.studyplanner.common.NotFoundException;
import de.studyplanner.demo.DemoAccount;
import de.studyplanner.user.User;
import de.studyplanner.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Geschäftslogik rund um Konten und Anmeldung.
 *
 * - Registrieren: E-Mail normalisieren, Passwort mit BCrypt hashen, speichern und eine Bestätigungsmail
 *   schicken. Anmelden kann man sich erst nach Klick auf den Link in der Mail. Ist die Adresse schon
 *   registriert, bekommt der Besitzer stattdessen einen Hinweis per Mail – die Antwort ist dieselbe.
 * - E-Mail bestätigen: Einmal-Token aus dem Mail-Link einlösen – danach meldet man sich ganz normal an
 * - Login: Sperre wegen Fehlversuchen prüfen, Passwort gegen den Hash prüfen, Bestätigung prüfen, dann
 *   kurzlebiges Access-Token (JWT) + langlebiges Refresh-Token (für das httpOnly-Cookie) ausgeben
 * - Erneuern / Abmelden: Refresh-Token rotieren bzw. widerrufen
 * - Passwort zurücksetzen: Einmal-Token einlösen, neues Passwort setzen, alle Geräte abmelden
 * - Aktueller Benutzer: ID aus dem JWT lesen (CurrentUser) und aus der Datenbank laden
 *
 * Wird vom AuthController aufgerufen; nutzt UserRepository, PasswordEncoder, JwtService, UserTokenService,
 * RefreshTokenService, MailService und LoginAttemptService.
 */
@Service
public class AuthService {

    static final Duration VERIFICATION_VALIDITY = Duration.ofHours(24);
    static final Duration PASSWORD_RESET_VALIDITY = Duration.ofMinutes(60);

    /** Innerhalb dieser Zeit wird höchstens eine Mail derselben Art an eine Adresse geschickt (gegen Mail-Spam). */
    private static final Duration MAIL_COOLDOWN = Duration.ofMinutes(1);

    /** Fehlercode: Refresh-Token wurde gerade von einem anderen Tab erneuert – mit neuem Cookie erneut versuchen. */
    static final String REFRESH_TOKEN_ROTATED = "REFRESH_TOKEN_ROTATED";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUser currentUser;
    private final UserTokenService userTokenService;
    private final MailService mailService;
    private final LoginAttemptService loginAttemptService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Hash eines Zufallspassworts. Bei unbekannter E-Mail wird das Passwort trotzdem dagegen geprüft,
     * damit der Login gleich lange dauert – sonst könnte man an der Antwortzeit erkennen, ob ein Konto existiert.
     */
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       CurrentUser currentUser, UserTokenService userTokenService, MailService mailService,
                       LoginAttemptService loginAttemptService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
        this.userTokenService = userTokenService;
        this.mailService = mailService;
        this.loginAttemptService = loginAttemptService;
        this.refreshTokenService = refreshTokenService;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-passwort-fuer-gleiche-antwortzeit");
    }

    /**
     * Legt einen neuen (noch unbestätigten) Benutzer an und schickt die Bestätigungsmail.
     *
     * Ist die Adresse schon registriert, wird KEIN Fehler gemeldet – sonst könnte jeder über das Formular
     * herausfinden, welche Adressen ein Konto haben. Stattdessen bekommt der Besitzer eine Mail
     * (siehe notifyExistingAccount), und die Antwort ist genau dieselbe wie bei einer neuen Adresse.
     * Das Passwort wird in beiden Fällen gehasht, damit auch die Antwortzeit nichts verrät.
     */
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        PasswordPolicy.requireValidLength(request.password());
        String passwordHash = passwordEncoder.encode(request.password());

        userRepository.findByEmail(email).ifPresentOrElse(
                this::notifyExistingAccount,
                () -> {
                    User user = userRepository.save(new User(email, passwordHash, request.displayName().trim()));
                    mailService.sendVerificationMail(user,
                            userTokenService.create(user, TokenPurpose.EMAIL_VERIFICATION, VERIFICATION_VALIDITY));
                });
        return new MessageResponse("Fast geschafft! Wir haben dir eine E-Mail an " + email + " geschickt. "
                + "Klicke auf den Link darin, um dein Konto zu aktivieren.");
    }

    /**
     * Jemand wollte sich mit einer schon registrierten Adresse registrieren:
     * - Konto noch unbestätigt → Bestätigungsmail erneut schicken (der Besitzer hat sie wohl nicht gefunden)
     * - Konto bestätigt → Hinweis "Du hast bereits ein Konto" mit Link zum Zurücksetzen des Passworts
     * Höchstens eine Mail pro Minute und Adresse; der Demo-Account bekommt nie Mails.
     */
    private void notifyExistingAccount(User user) {
        if (DemoAccount.isDemo(user.getEmail())) {
            return;
        }
        if (!user.isEmailVerified()) {
            if (!userTokenService.createdWithin(user, TokenPurpose.EMAIL_VERIFICATION, MAIL_COOLDOWN)) {
                mailService.sendVerificationMail(user,
                        userTokenService.create(user, TokenPurpose.EMAIL_VERIFICATION, VERIFICATION_VALIDITY));
            }
        } else if (!userTokenService.createdWithin(user, TokenPurpose.PASSWORD_RESET, MAIL_COOLDOWN)) {
            mailService.sendAccountExistsMail(user,
                    userTokenService.create(user, TokenPurpose.PASSWORD_RESET, PASSWORD_RESET_VALIDITY));
        }
    }

    /**
     * Prüft die Zugangsdaten und liefert bei Erfolg ein neues Access-Token und ein neues Refresh-Token.
     *
     * Reihenfolge: 1. Sperre wegen zu vieler Fehlversuche (HTTP 429) → 2. Passwort (HTTP 401, gleiche Meldung
     * für unbekannte E-Mail und falsches Passwort) → 3. E-Mail bestätigt? (HTTP 403). Schritt 3 kommt bewusst
     * erst nach der Passwortprüfung, damit nur der Kontoinhaber erfährt, dass die Bestätigung fehlt.
     */
    @Transactional
    public AuthSession login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        loginAttemptService.checkNotLocked(email);

        User user = userRepository.findByEmail(email).orElse(null);
        boolean passwordCorrect = !PasswordPolicy.tooLong(request.password())
                && passwordEncoder.matches(request.password(), user != null ? user.getPasswordHash() : dummyPasswordHash);
        if (user == null || !passwordCorrect) {
            loginAttemptService.loginFailed(email);
            throw new InvalidCredentialsException("E-Mail oder Passwort ist falsch.");
        }
        loginAttemptService.reset(email);

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Bitte bestätige zuerst deine E-Mail-Adresse. "
                    + "Den Link dazu haben wir dir bei der Registrierung geschickt.");
        }
        return startSession(user);
    }

    /**
     * Erneuert die Anmeldung mit dem Refresh-Token aus dem Cookie: neues Access-Token + neues Refresh-Token
     * (das alte wird ungültig). Ohne gültiges Token → HTTP 401; war das Token gerade erst ersetzt worden
     * (anderer Browser-Tab), enthält die Antwort den Code REFRESH_TOKEN_ROTATED.
     *
     * noRollbackFor: Wird ein gestohlenes Token erkannt, widerruft rotate() alle Sitzungen und danach wird
     * hier HTTP 401 geworfen. Ohne diese Einstellung würde der Fehler die Transaktion zurückrollen –
     * und damit auch das Widerrufen.
     */
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public AuthSession refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidCredentialsException("Nicht angemeldet.");
        }
        return switch (refreshTokenService.rotate(refreshToken)) {
            case RefreshTokenService.Rotation.Rotated(User user, String newToken) ->
                    new AuthSession(newResponse(user), newToken);
            case RefreshTokenService.Rotation.RecentlyRotated() ->
                    throw new InvalidCredentialsException("Die Anmeldung wurde gerade erneuert.", REFRESH_TOKEN_ROTATED);
            case RefreshTokenService.Rotation.Invalid() ->
                    throw new InvalidCredentialsException("Deine Sitzung ist abgelaufen. Bitte melde dich erneut an.");
        };
    }

    /** Abmelden auf diesem Gerät: widerruft das Refresh-Token aus dem Cookie (falls vorhanden). */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }
    }

    /**
     * Löst das Token aus der Bestätigungsmail ein und aktiviert das Konto. Angemeldet wird dabei bewusst
     * nicht – der Benutzer landet auf der Login-Seite und meldet sich dort mit seinem Passwort an.
     *
     * @throws de.studyplanner.common.BadRequestException wenn der Link ungültig oder abgelaufen ist (HTTP 400)
     */
    @Transactional
    public MessageResponse verifyEmail(String token) {
        User user = userTokenService.consume(token, TokenPurpose.EMAIL_VERIFICATION);
        user.markEmailVerified();
        return new MessageResponse("Deine E-Mail-Adresse ist bestätigt. Du kannst dich jetzt anmelden.");
    }

    /**
     * Schickt eine neue Bestätigungsmail – aber nur, wenn es ein unbestätigtes Konto mit der Adresse gibt.
     * Die Antwort ist immer gleich, damit man so nicht herausfinden kann, welche Adressen registriert sind.
     */
    @Transactional
    public MessageResponse resendVerification(String email) {
        userRepository.findByEmail(normalizeEmail(email))
                .filter(user -> !user.isEmailVerified())
                .filter(user -> !userTokenService.createdWithin(user, TokenPurpose.EMAIL_VERIFICATION, MAIL_COOLDOWN))
                .ifPresent(user -> mailService.sendVerificationMail(user,
                        userTokenService.create(user, TokenPurpose.EMAIL_VERIFICATION, VERIFICATION_VALIDITY)));
        return new MessageResponse("Falls ein unbestätigtes Konto mit dieser E-Mail-Adresse existiert, "
                + "haben wir dir eine neue Bestätigungsmail geschickt.");
    }

    /**
     * "Passwort vergessen": schickt einen Link zum Zurücksetzen, falls ein Konto existiert
     * (nie für den Demo-Account). Die Antwort ist immer gleich (siehe resendVerification).
     */
    @Transactional
    public MessageResponse forgotPassword(String email) {
        userRepository.findByEmail(normalizeEmail(email))
                .filter(user -> !DemoAccount.isDemo(user.getEmail()))
                .filter(user -> !userTokenService.createdWithin(user, TokenPurpose.PASSWORD_RESET, MAIL_COOLDOWN))
                .ifPresent(user -> mailService.sendPasswordResetMail(user,
                        userTokenService.create(user, TokenPurpose.PASSWORD_RESET, PASSWORD_RESET_VALIDITY)));
        return new MessageResponse("Falls ein Konto mit dieser E-Mail-Adresse existiert, haben wir dir einen Link "
                + "zum Zurücksetzen des Passworts geschickt. Schau auch im Spam-Ordner nach.");
    }

    /**
     * Setzt mit dem Token aus der Mail ein neues Passwort. Weil der Link per Mail kam, gilt die
     * E-Mail-Adresse danach auch als bestätigt; eine eventuelle Login-Sperre wird aufgehoben.
     * Alle bestehenden Anmeldungen (Refresh-Tokens) werden beendet – wer das alte Passwort kannte,
     * ist danach überall abgemeldet.
     */
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordPolicy.requireValidLength(request.password());
        User user = userTokenService.consume(request.token(), TokenPurpose.PASSWORD_RESET);
        DemoAccount.requireNotDemo(user.getEmail());
        user.changePasswordHash(passwordEncoder.encode(request.password()));
        user.markEmailVerified();
        loginAttemptService.reset(user.getEmail());
        refreshTokenService.revokeAll(user.getId());
        return new MessageResponse("Dein Passwort wurde geändert. Du kannst dich jetzt mit dem neuen Passwort anmelden.");
    }

    /** Lädt den angemeldeten Benutzer anhand der ID aus dem JWT. */
    @Transactional(readOnly = true)
    public UserResponse me() {
        return userRepository.findById(currentUser.id())
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("Benutzer nicht gefunden."));
    }

    /** Neue Anmeldung: Access-Token (JWT) für die Antwort und Refresh-Token für das Cookie. */
    AuthSession startSession(User user) {
        return new AuthSession(newResponse(user), refreshTokenService.issue(user));
    }

    /** Antwort-JSON mit neuem Access-Token und den Benutzerdaten. */
    private AuthResponse newResponse(User user) {
        return new AuthResponse(jwtService.createToken(user), UserResponse.from(user));
    }

    /**
     * E-Mails werden ohne Leerzeichen und kleingeschrieben gespeichert – so klappt der Login unabhängig von Groß-/Kleinschreibung.
     */
    static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
