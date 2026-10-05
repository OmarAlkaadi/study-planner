package de.studyplanner.auth;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/** Datenbankzugriff für die Einmal-Tokens aus E-Mail-Links (Spring Data JPA). */
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    /** Sucht ein Token anhand seines Hashs (inkl. Benutzer per JOIN). */
    @EntityGraph(attributePaths = "user")
    Optional<UserToken> findByTokenHashAndPurpose(String tokenHash, TokenPurpose purpose);

    /** true, wenn für den Benutzer seit {@code since} schon ein Token dieser Art erstellt wurde (gegen Mail-Spam). */
    boolean existsByUserIdAndPurposeAndCreatedAtAfter(Long userId, TokenPurpose purpose, LocalDateTime since);

    /** Löscht alle Tokens einer Art für einen Benutzer – alte Links werden damit ungültig. */
    @Modifying
    @Query("delete from UserToken t where t.user.id = :userId and t.purpose = :purpose")
    void deleteByUserIdAndPurpose(@Param("userId") Long userId, @Param("purpose") TokenPurpose purpose);

    /** Löscht alle abgelaufenen Tokens (nächtliches Aufräumen). */
    @Modifying
    @Query("delete from UserToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
