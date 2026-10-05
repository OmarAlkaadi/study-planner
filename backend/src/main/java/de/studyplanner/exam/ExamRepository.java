package de.studyplanner.exam;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Datenbankzugriff für Klausuren (Spring Data JPA). Alle Abfragen sind auf eine userId beschränkt.
 * JpaSpecificationExecutor ermöglicht die dynamischen Filter aus ExamSpecifications.
 */
public interface ExamRepository extends JpaRepository<Exam, Long>, JpaSpecificationExecutor<Exam> {

    /**
     * Gefilterte Liste (Filter siehe ExamSpecifications). Überschreibt die Standardmethode nur, um per
     * @EntityGraph den Kurs direkt mitzuladen (JOIN) – so entstehen keine N+1-Abfragen.
     */
    @Override
    @EntityGraph(attributePaths = "course")
    List<Exam> findAll(Specification<Exam> spec, Sort sort);

    /** Eine Klausur – nur wenn sie dem Benutzer gehört (inkl. Kurs per JOIN). */
    @EntityGraph(attributePaths = "course")
    Optional<Exam> findByIdAndUserId(Long id, Long userId);

    /** Klausuren im Zeitraum [from, to) – für den Kalender. */
    @EntityGraph(attributePaths = "course")
    List<Exam> findByUserIdAndExamDateGreaterThanEqualAndExamDateLessThanOrderByExamDateAsc(
            Long userId, LocalDateTime from, LocalDateTime to);

    /** Die nächsten 5 Klausuren ab einem Zeitpunkt – für das Dashboard. */
    @EntityGraph(attributePaths = "course")
    List<Exam> findTop5ByUserIdAndExamDateGreaterThanEqualOrderByExamDateAsc(Long userId, LocalDateTime from);

    /** Notendurchschnitt aller bewerteten Klausuren (SQL: AVG). null, wenn es noch keine Noten gibt. */
    @Query("select avg(e.grade) from Exam e where e.user.id = :userId and e.grade is not null")
    Double averageGrade(@Param("userId") Long userId);
}
