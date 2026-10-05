package de.studyplanner.auth;

import de.studyplanner.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * JPA-Entity für die Tabelle "refresh_tokens": eine Anmeldung auf einem Gerät/Browser.
 *
 * Das Token selbst liegt nur im httpOnly-Cookie des Browsers; hier steht nur sein SHA-256-Hash.
 * Ein Refresh-Token ist gültig, solange es nicht abgelaufen und nicht widerrufen ist (revokedAt == null).
 * Widerrufen wird es beim Erneuern (Rotation, dann zusätzlich replacedAt), beim Abmelden und beim
 * Ändern/Zurücksetzen des Passworts.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Zeitpunkt des Widerrufs – null, solange das Token gültig ist. */
    private LocalDateTime revokedAt;

    /**
     * Zeitpunkt, zu dem das Token durch Erneuern ersetzt wurde – null, wenn es nie ersetzt wurde
     * (z. B. nur abgemeldet). Nur ein ersetztes Token, das später wieder auftaucht, gilt als gestohlen.
     */
    private LocalDateTime replacedAt;

    protected RefreshToken() {
        // für JPA
    }

    public RefreshToken(User user, String tokenHash, LocalDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    /** Setzt das Erstellungsdatum automatisch vor dem ersten Speichern. */
    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** Markiert das Token als widerrufen – danach kann es nicht mehr zum Erneuern benutzt werden. */
    public void revoke() {
        if (revokedAt == null) {
            revokedAt = LocalDateTime.now();
        }
    }

    /** Markiert das Token als durch Erneuern ersetzt (Rotation) und damit auch als widerrufen. */
    public void markReplaced() {
        revoke();
        replacedAt = LocalDateTime.now();
    }

    // Getter
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public LocalDateTime getReplacedAt() {
        return replacedAt;
    }
}
