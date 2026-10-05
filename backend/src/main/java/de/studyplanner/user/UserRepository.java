package de.studyplanner.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Datenbankzugriff für Benutzer (Spring Data JPA).
 *
 * Die Implementierung erzeugt Spring automatisch zur Laufzeit. Die SQL-Abfrage wird aus dem
 * Methodennamen abgeleitet, z. B. findByEmail → SELECT * FROM users WHERE email = ?.
 * Standardmethoden wie save, findById oder getReferenceById erbt das Interface von JpaRepository.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Sucht einen Benutzer per E-Mail (für den Login). */
    Optional<User> findByEmail(String email);

    /** Prüft, ob die E-Mail schon vergeben ist (beim Anlegen der Demo-Daten). */
    boolean existsByEmail(String email);

    /**
     * Löscht einen Benutzer per E-Mail direkt in der Datenbank (für das nächtliche Zurücksetzen der Demo).
     * Alle seine Daten löscht die Datenbank per ON DELETE CASCADE mit.
     */
    @Modifying
    @Query("delete from User u where u.email = :email")
    int deleteByEmail(@Param("email") String email);

    /**
     * Löscht Konten, deren E-Mail-Adresse nie bestätigt wurde und die vor dem Stichtag angelegt wurden
     * (nächtliches Aufräumen). So bleibt eine fremde Adresse nicht dauerhaft "belegt".
     */
    @Modifying
    @Query("delete from User u where u.emailVerified = false and u.createdAt < :before")
    int deleteUnverifiedCreatedBefore(@Param("before") LocalDateTime before);
}
