package de.studyplanner.dashboard;

/** Aufgabenfortschritt eines Kurses: erledigte von allen Aufgaben. */
public record CourseTaskProgress(Long courseId, String courseName, String color, Long total, Long done) {
}
