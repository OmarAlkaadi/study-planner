package de.studyplanner.dashboard;

/** Lernzeit (Summe in Minuten) für einen Kurs. */
public record CourseMinutes(Long courseId, String courseName, String color, Long minutes) {
}
