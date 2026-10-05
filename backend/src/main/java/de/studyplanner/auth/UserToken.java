package de.studyplanner.auth;

import de.studyplanner.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * JPA-Entity für die Tabelle "user_tokens": ein Einmal-Token aus einem E-Mail-Link
 * (E-Mail bestätigen oder Passwort zurücksetzen).
 *
 * Gespeichert wird nur der SHA-256-Hash des Tokens, nie das Token selbst (siehe UserTokenService).
 * Ein Token ist nur einmal und nur bis expiresAt verwendbar. Wird der Benutzer gelöscht,
 * löscht die Datenbank seine Tokens mit (ON DELETE CASCADE).
 */
@Entity
@Table(name = "user_tokens")
public class UserToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenPurpose purpose;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** Zeitpunkt der Verwendung – null, solange das Token noch nicht benutzt wurde. */
    private LocalDateTime usedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserToken() {
        // für JPA
    }

    public UserToken(User user, String tokenHash, TokenPurpose purpose, LocalDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }

    /** Setzt das Erstellungsdatum automatisch vor dem ersten Speichern. */
    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** true, wenn das Token noch nicht benutzt und noch nicht abgelaufen ist. */
    public boolean isUsable(LocalDateTime now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    /** Markiert das Token als benutzt – danach funktioniert der Link nicht mehr. */
    public void markUsed() {
        usedAt = LocalDateTime.now();
    }

    // Getter
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public TokenPurpose getPurpose() {
        return purpose;
    }
}
