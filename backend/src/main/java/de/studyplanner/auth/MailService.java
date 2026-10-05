package de.studyplanner.auth;

import de.studyplanner.user.User;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Verschickt die E-Mails der App (Bestätigung nach der Registrierung, Passwort zurücksetzen,
 * Hinweis "Konto existiert bereits").
 *
 * Versand-Zeitpunkt: Läuft gerade eine Datenbank-Transaktion, wird die Mail erst NACH dem erfolgreichen
 * Speichern verschickt – es geht also nie ein Link für ein Konto raus, das gar nicht gespeichert wurde.
 * Verschickt wird im Hintergrund (app.mail.async), damit ein langsamer Mailserver keine Anfrage bremst.
 * Schlägt der Versand fehl, wird das nur geloggt; der Benutzer kann die Mail erneut anfordern.
 *
 * Der Mailserver wird in application.yml unter spring.mail.* konfiguriert (Umgebungsvariablen MAIL_HOST,
 * MAIL_PORT, MAIL_USERNAME, ...). Mit Docker Compose ist das standardmäßig Mailpit – ein Test-Postfach,
 * das alle Mails abfängt (http://localhost:8025). Die Links zeigen auf app.frontend-url (APP_FRONTEND_URL).
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String frontendUrl;
    private final boolean async;
    /** Hintergrund-Threads für den Versand (virtuelle Threads – leichtgewichtig, einer pro Mail). */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public MailService(JavaMailSender mailSender,
                       @Value("${app.mail.from}") String from,
                       @Value("${app.frontend-url}") String frontendUrl,
                       @Value("${app.mail.async:true}") boolean async) {
        this.mailSender = mailSender;
        this.from = from;
        // Schrägstrich am Ende entfernen, damit keine Links wie "...3000//verify-email" entstehen
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
        this.async = async;
    }

    /** Mail mit dem Link zum Bestätigen der E-Mail-Adresse (nach der Registrierung). */
    public void sendVerificationMail(User user, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        send(user.getEmail(), "Bitte bestätige deine E-Mail-Adresse", """
                Hallo %s,

                willkommen beim Study Planner! Bitte bestätige deine E-Mail-Adresse, indem du diesen Link öffnest:

                %s

                Der Link ist 24 Stunden gültig. Falls du dich nicht registriert hast, kannst du diese E-Mail ignorieren.

                Viele Grüße
                Dein Study Planner
                """.formatted(user.getDisplayName(), link));
    }

    /** Mail mit dem Link zum Setzen eines neuen Passworts ("Passwort vergessen"). */
    public void sendPasswordResetMail(User user, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        send(user.getEmail(), "Passwort zurücksetzen", """
                Hallo %s,

                du hast angefordert, dein Passwort zurückzusetzen. Über diesen Link kannst du ein neues Passwort festlegen:

                %s

                Der Link ist 60 Minuten gültig und funktioniert nur einmal.
                Falls du das nicht warst, ignoriere diese E-Mail – dein Passwort bleibt dann unverändert.

                Viele Grüße
                Dein Study Planner
                """.formatted(user.getDisplayName(), link));
    }

    /**
     * Hinweis an den Besitzer, wenn sich jemand mit seiner (schon registrierten) Adresse erneut registrieren
     * wollte. Die Registrierungsseite zeigt in diesem Fall dieselbe Meldung wie bei einer neuen Adresse – so
     * verrät sie nicht, welche Adressen registriert sind. Enthält direkt einen Link zum Zurücksetzen des Passworts.
     */
    public void sendAccountExistsMail(User user, String passwordResetToken) {
        String loginLink = frontendUrl + "/login";
        String resetLink = frontendUrl + "/reset-password?token=" + passwordResetToken;
        send(user.getEmail(), "Du hast bereits ein Konto", """
                Hallo %s,

                jemand – vermutlich du – wollte sich gerade mit dieser E-Mail-Adresse registrieren.
                Für diese Adresse gibt es aber schon ein Konto. Du kannst dich hier anmelden:

                %s

                Passwort vergessen? Mit diesem Link legst du ein neues fest (60 Minuten gültig):

                %s

                Falls du das nicht warst, kannst du diese E-Mail ignorieren – an deinem Konto ändert sich nichts.

                Viele Grüße
                Dein Study Planner
                """.formatted(user.getDisplayName(), loginLink, resetLink));
    }

    /**
     * Baut die Mail und verschickt sie nach dem erfolgreichen Ende der aktuellen Transaktion
     * (ohne Transaktion sofort). Wird die Transaktion zurückgerollt, geht keine Mail raus.
     */
    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(message);
                }
            });
        } else {
            dispatch(message);
        }
    }

    /** Im Hintergrund oder direkt versenden (Tests: direkt, damit sie die Mail sofort auslesen können). */
    private void dispatch(SimpleMailMessage message) {
        if (async) {
            executor.execute(() -> deliver(message));
        } else {
            deliver(message);
        }
    }

    /** Der eigentliche Versand. Fehler werden nur geloggt – die Anfrage des Benutzers ist längst beantwortet. */
    private void deliver(SimpleMailMessage message) {
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("E-Mail \"{}\" konnte nicht gesendet werden: {}", message.getSubject(), e.getMessage());
        }
    }

    /** Beim Herunterfahren auf noch laufende Versandvorgänge warten. */
    @PreDestroy
    void shutdown() {
        executor.close();
    }
}
