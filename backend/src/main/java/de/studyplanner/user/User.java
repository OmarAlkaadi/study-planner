package de.studyplanner.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * JPA-Entity für die Tabelle "users": ein registriertes Konto.
 *
 * Alle anderen Tabellen (courses, exams, tasks, study_sessions) verweisen über user_id auf diese Tabelle.
 * Das Passwort wird nie im Klartext gespeichert, sondern nur als BCrypt-Hash.
 * Die Spaltennamen ergeben sich aus den Feldnamen (passwordHash → password_hash).
 * Neue Konten sind erst nach Klick auf den Link in der Bestätigungsmail aktiv (emailVerified).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** true, sobald der Benutzer den Link in der Bestätigungsmail angeklickt hat. Ohne Bestätigung kein Login. */
    @Column(nullable = false)
    private boolean emailVerified;

    protected User() {
        // für JPA
    }

    public User(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    /** Wird von JPA automatisch direkt vor dem ersten Speichern aufgerufen und setzt das Erstellungsdatum. */
    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** Markiert die E-Mail-Adresse als bestätigt (nach Klick auf den Link in der Mail). */
    public void markEmailVerified() {
        this.emailVerified = true;
    }

    /** Neuer Anzeigename (Seite "Konto"). */
    public void changeDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /** Neues Passwort – erwartet bereits den BCrypt-Hash, nie das Klartext-Passwort. */
    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    // Getter (geändert wird nur über die sprechenden Methoden oben, die E-Mail bleibt fest)
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }
}
