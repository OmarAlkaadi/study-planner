package de.studyplanner.account;

import de.studyplanner.auth.PasswordPolicy;
import de.studyplanner.auth.RefreshTokenService;
import de.studyplanner.auth.UserResponse;
import de.studyplanner.common.BadRequestException;
import de.studyplanner.common.CurrentUser;
import de.studyplanner.common.NotFoundException;
import de.studyplanner.demo.DemoAccount;
import de.studyplanner.user.User;
import de.studyplanner.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Geschäftslogik für die Seite "Konto": Anzeigenamen ändern, Passwort ändern, Konto löschen.
 *
 * Passwort ändern und Konto löschen verlangen das aktuelle Passwort – so kann niemand, der nur kurz
 * an einem fremden, angemeldeten Rechner sitzt, das Konto übernehmen oder löschen.
 * Ein falsches Passwort führt hier zu HTTP 400 (nicht 401), damit das Frontend nicht automatisch abmeldet.
 * Nach einer Passwortänderung werden alle anderen Geräte abgemeldet (Refresh-Tokens widerrufen).
 * Beim öffentlichen Demo-Account ist alles davon gesperrt (HTTP 403, siehe DemoAccount).
 */
@Service
@Transactional
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;
    private final RefreshTokenService refreshTokenService;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder, CurrentUser currentUser,
                          RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
        this.refreshTokenService = refreshTokenService;
    }

    /** Ändert den Anzeigenamen; die Transaktion speichert die Änderung automatisch. */
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = loadCurrentUser();
        user.changeDisplayName(request.displayName().trim());
        return UserResponse.from(user);
    }

    /**
     * Ändert das Passwort, nachdem das aktuelle Passwort geprüft wurde. Danach werden alle Anmeldungen
     * beendet (auch auf anderen Geräten); für dieses Gerät wird direkt ein neues Refresh-Token ausgegeben,
     * damit man hier angemeldet bleibt.
     *
     * @return neues Refresh-Token für das Cookie dieses Geräts
     */
    public String changePassword(ChangePasswordRequest request) {
        User user = loadCurrentUser();
        requireCorrectPassword(user, request.currentPassword());
        PasswordPolicy.requireValidLength(request.newPassword());
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAll(user.getId());
        return refreshTokenService.issue(user);
    }

    /**
     * Löscht das Konto endgültig. Kurse, Klausuren, Aufgaben, Lerneinheiten und alle Tokens löscht die
     * Datenbank automatisch mit (ON DELETE CASCADE an allen Fremdschlüsseln auf users).
     */
    public void deleteAccount(DeleteAccountRequest request) {
        User user = loadCurrentUser();
        requireCorrectPassword(user, request.password());
        userRepository.delete(user);
    }

    /**
     * Lädt den angemeldeten Benutzer (404, falls er inzwischen gelöscht wurde). Alle Aktionen dieser Klasse
     * ändern das Konto – deshalb wird hier zentral geprüft, dass es nicht der Demo-Account ist.
     */
    private User loadCurrentUser() {
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new NotFoundException("Benutzer nicht gefunden."));
        DemoAccount.requireNotDemo(user.getEmail());
        return user;
    }

    /** Wirft BadRequestException (HTTP 400), wenn das eingegebene Passwort nicht stimmt. */
    private void requireCorrectPassword(User user, String password) {
        if (PasswordPolicy.tooLong(password) || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadRequestException("Das Passwort ist falsch.");
        }
    }
}
