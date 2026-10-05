package de.studyplanner.auth;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/** Datenbankzugriff für Refresh-Tokens (Spring Data JPA). */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** Sucht ein Refresh-Token anhand seines Hashs (inkl. Benutzer per JOIN). */
    @EntityGraph(attributePaths = "user")
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Widerruft alle noch gültigen Refresh-Tokens eines Benutzers – meldet ihn auf allen Geräten ab. */
    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.user.id = :userId and t.revokedAt is null")
    int revokeAllForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Räumt auf: löscht abgelaufene Tokens eines Benutzers und solche, die schon länger abgemeldet bzw.
     * widerrufen sind. Durch Erneuern ersetzte Tokens bleiben bis zu ihrem Ablauf stehen – nur so lässt sich
     * erkennen, wenn ein gestohlenes, längst ersetztes Token später noch einmal auftaucht.
     */
    @Modifying
    @Query("""
            delete from RefreshToken t
            where t.user.id = :userId
              and (t.expiresAt < :now or (t.revokedAt < :revokedBefore and t.replacedAt is null))
            """)
    void deleteStale(@Param("userId") Long userId, @Param("now") LocalDateTime now,
                     @Param("revokedBefore") LocalDateTime revokedBefore);

    /** Wie deleteStale, aber für alle Benutzer auf einmal (nächtliches Aufräumen). */
    @Modifying
    @Query("""
            delete from RefreshToken t
            where t.expiresAt < :now or (t.revokedAt < :revokedBefore and t.replacedAt is null)
            """)
    int deleteStaleForAll(@Param("now") LocalDateTime now, @Param("revokedBefore") LocalDateTime revokedBefore);
}
