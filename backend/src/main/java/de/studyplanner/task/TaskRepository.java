package de.studyplanner.task;

import de.studyplanner.dashboard.CourseTaskProgress;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Datenbankzugriff für Aufgaben (Spring Data JPA).
 * JpaSpecificationExecutor ermöglicht dynamische Filter (siehe TaskSpecifications): Nur die gesetzten
 * Filter landen in der WHERE-Klausel. Die count...-Methoden liefern Kennzahlen für das Dashboard.
 */
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    /** Überschreibt die Standardmethode nur, um per @EntityGraph den Kurs direkt mitzuladen (kein N+1). */
    @Override
    @EntityGraph(attributePaths = "course")
    List<Task> findAll(Specification<Task> spec, Sort sort);

    /** Eine Aufgabe – nur wenn sie dem Benutzer gehört. */
    @EntityGraph(attributePaths = "course")
    Optional<Task> findByIdAndUserId(Long id, Long userId);

    /** Anzahl aller Aufgaben des Benutzers. */
    long countByUserId(Long userId);

    /** Anzahl der Aufgaben mit einem bestimmten Status. */
    long countByUserIdAndStatus(Long userId, TaskStatus status);

    /** Anzahl nicht erledigter Aufgaben, deren Deadline vor dem Datum liegt (= überfällig). */
    long countByUserIdAndStatusNotAndDueDateBefore(Long userId, TaskStatus status, LocalDate date);

    /** Nicht erledigte Aufgaben bis einschließlich Datum (inkl. überfällige) – für das Dashboard. */
    @EntityGraph(attributePaths = "course")
    List<Task> findByUserIdAndStatusNotAndDueDateLessThanEqualOrderByDueDateAsc(
            Long userId, TaskStatus status, LocalDate date);

    /** Aufgaben mit Deadline im Zeitraum – für den Kalender. */
    @EntityGraph(attributePaths = "course")
    List<Task> findByUserIdAndDueDateBetween(Long userId, LocalDate from, LocalDate to);

    /** Erledigte und gesamte Aufgaben pro Kurs (SQL: GROUP BY). */
    @Query("""
            select new de.studyplanner.dashboard.CourseTaskProgress(
                c.id, c.name, c.color, count(t), sum(case when t.status = :done then 1 else 0 end))
            from Task t join t.course c
            where t.user.id = :userId
            group by c.id, c.name, c.color
            order by c.name
            """)
    List<CourseTaskProgress> taskProgressPerCourse(@Param("userId") Long userId, @Param("done") TaskStatus done);
}
