package de.studyplanner.task;

import org.springframework.data.jpa.domain.Specification;

/**
 * Bausteine für die dynamische Filterung der Aufgaben (JPA Criteria API).
 * Nur Filter, die tatsächlich gesetzt sind, landen in der SQL-WHERE-Klausel.
 */
public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    /** WHERE user_id = ? (immer gesetzt). */
    public static Specification<Task> belongsTo(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    /** WHERE status = ? */
    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** WHERE priority = ? */
    public static Specification<Task> hasPriority(TaskPriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    /** WHERE course_id = ? */
    public static Specification<Task> hasCourse(Long courseId) {
        return (root, query, cb) -> cb.equal(root.get("course").get("id"), courseId);
    }

    /** Sucht in Titel und Beschreibung (pattern siehe TextUtils.likePattern). */
    public static Specification<Task> matchesText(String pattern) {
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.<String>get("title")), pattern),
                cb.like(cb.lower(root.<String>get("description")), pattern)
        );
    }
}
