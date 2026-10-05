package de.studyplanner.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

/**
 * Datenbankzugriff für Kurse (Spring Data JPA). Die Abfragen werden aus den Methodennamen erzeugt;
 * jede Abfrage enthält die userId, damit nur Kurse des angemeldeten Benutzers gefunden werden.
 */
public interface CourseRepository extends JpaRepository<Course, Long> {

    /** Alle Kurse eines Benutzers (Kursliste, Kalender). */
    List<Course> findByUserId(Long userId);

    /** Ein Kurs – aber nur, wenn er dem Benutzer gehört (sonst leer → 404). */
    Optional<Course> findByIdAndUserId(Long id, Long userId);

    /** Kurse an einem bestimmten Wochentag (Dashboard: "Heute"). */
    List<Course> findByUserIdAndDayOfWeek(Long userId, DayOfWeek dayOfWeek);

    /** Anzahl der Kurse (Kennzahl im Dashboard). */
    long countByUserId(Long userId);

    /** Suche in Name und Dozent (pattern z. B. "%mathe%", siehe TextUtils.likePattern). */
    @Query("""
            select c from Course c
            where c.user.id = :userId
              and (lower(c.name) like :pattern or lower(c.lecturer) like :pattern)
            """)
    List<Course> search(@Param("userId") Long userId, @Param("pattern") String pattern);
}
