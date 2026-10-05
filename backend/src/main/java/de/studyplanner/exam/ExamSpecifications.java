package de.studyplanner.exam;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Bausteine für die dynamische Filterung der Klausuren (JPA Criteria API) – gleiches Muster wie
 * TaskSpecifications. Nur Filter, die tatsächlich gesetzt sind, landen in der SQL-WHERE-Klausel;
 * die Datenbank liefert also nur die passenden Klausuren statt alle.
 */
public final class ExamSpecifications {

    private ExamSpecifications() {
    }

    /** WHERE user_id = ? (immer gesetzt). */
    public static Specification<Exam> belongsTo(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    /** WHERE course_id = ? */
    public static Specification<Exam> hasCourse(Long courseId) {
        return (root, query, cb) -> cb.equal(root.get("course").get("id"), courseId);
    }

    /** Anstehend: WHERE exam_date >= jetzt. */
    public static Specification<Exam> notBefore(LocalDateTime time) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("examDate"), time);
    }

    /** Vergangen: WHERE exam_date < jetzt. */
    public static Specification<Exam> before(LocalDateTime time) {
        return (root, query, cb) -> cb.lessThan(root.get("examDate"), time);
    }

    /** Sucht in Klausurtitel und Kursname (pattern siehe TextUtils.likePattern). */
    public static Specification<Exam> matchesText(String pattern) {
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.<String>get("title")), pattern),
                cb.like(cb.lower(root.get("course").<String>get("name")), pattern)
        );
    }
}
