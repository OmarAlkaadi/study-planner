package de.studyplanner.exam;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Klausurdaten für das Frontend – mit Name und Farbe des Kurses, damit das Frontend
 * keine zusätzliche Anfrage für den Kurs braucht.
 */
public record ExamResponse(
        Long id,
        Long courseId,
        String courseName,
        String courseColor,
        String title,
        LocalDateTime examDate,
        String room,
        String notes,
        BigDecimal grade
) {

    /** Wandelt die Exam-Entity in dieses DTO um. */
    public static ExamResponse from(Exam exam) {
        return new ExamResponse(
                exam.getId(),
                exam.getCourse().getId(),
                exam.getCourse().getName(),
                exam.getCourse().getColor(),
                exam.getTitle(),
                exam.getExamDate(),
                exam.getRoom(),
                exam.getNotes(),
                exam.getGrade()
        );
    }
}
