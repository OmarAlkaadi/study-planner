package de.studyplanner.studysession;

import java.time.LocalDate;

/** Lerneinheit für das Frontend, inkl. Name und Farbe des Kurses. */
public record StudySessionResponse(
        Long id,
        Long courseId,
        String courseName,
        String courseColor,
        LocalDate sessionDate,
        Integer durationMinutes,
        String topic
) {

    /** Wandelt die Entity in dieses DTO um. */
    public static StudySessionResponse from(StudySession session) {
        return new StudySessionResponse(
                session.getId(),
                session.getCourse().getId(),
                session.getCourse().getName(),
                session.getCourse().getColor(),
                session.getSessionDate(),
                session.getDurationMinutes(),
                session.getTopic()
        );
    }
}
