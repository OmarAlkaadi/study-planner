package de.studyplanner.task;

import de.studyplanner.course.Course;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Aufgabendaten für das Frontend, inkl. Kursname/-farbe (falls vorhanden) und berechnetem "overdue". */
public record TaskResponse(
        Long id,
        String title,
        String description,
        Long courseId,
        String courseName,
        String courseColor,
        LocalDate dueDate,
        TaskPriority priority,
        TaskStatus status,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        boolean overdue
) {

    /** Wandelt die Task-Entity in dieses DTO um (Kursfelder null, wenn kein Kurs). */
    public static TaskResponse from(Task task) {
        Course course = task.getCourse();
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                course != null ? course.getId() : null,
                course != null ? course.getName() : null,
                course != null ? course.getColor() : null,
                task.getDueDate(),
                task.getPriority(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getCompletedAt(),
                task.isOverdue()
        );
    }
}
