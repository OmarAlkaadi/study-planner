package de.studyplanner.studysession;

import de.studyplanner.dashboard.CourseMinutes;
import de.studyplanner.dashboard.DailyMinutes;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Datenbankzugriff für Lerneinheiten (Spring Data JPA) inkl. Aggregationen für die Statistiken.
 * Die @Query-Abfragen sind JPQL und erzeugen mit "select new ..." direkt die DTOs des Dashboards.
 */
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    /** Alle Lerneinheiten, neueste zuerst (Kurs per JOIN mitgeladen). */
    @EntityGraph(attributePaths = "course")
    List<StudySession> findByUserIdOrderBySessionDateDescIdDesc(Long userId);

    /** Wie oben, aber nur für einen Kurs. */
    @EntityGraph(attributePaths = "course")
    List<StudySession> findByUserIdAndCourseIdOrderBySessionDateDescIdDesc(Long userId, Long courseId);

    /** Eine Lerneinheit – nur wenn sie dem Benutzer gehört. */
    @EntityGraph(attributePaths = "course")
    Optional<StudySession> findByIdAndUserId(Long id, Long userId);

    /** Gesamte Lernzeit pro Kurs (SQL: SUM + GROUP BY). */
    @Query("""
            select new de.studyplanner.dashboard.CourseMinutes(c.id, c.name, c.color, sum(s.durationMinutes))
            from StudySession s join s.course c
            where s.user.id = :userId
            group by c.id, c.name, c.color
            order by sum(s.durationMinutes) desc
            """)
    List<CourseMinutes> minutesPerCourse(@Param("userId") Long userId);

    /** Lernzeit pro Tag im Zeitraum (Tage ohne Lerneinheit fehlen hier und werden im Service ergänzt). */
    @Query("""
            select new de.studyplanner.dashboard.DailyMinutes(s.sessionDate, sum(s.durationMinutes))
            from StudySession s
            where s.user.id = :userId and s.sessionDate between :from and :to
            group by s.sessionDate
            order by s.sessionDate
            """)
    List<DailyMinutes> minutesPerDay(@Param("userId") Long userId,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);
}
