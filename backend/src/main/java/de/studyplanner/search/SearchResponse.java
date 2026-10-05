package de.studyplanner.search;

import de.studyplanner.course.CourseResponse;
import de.studyplanner.exam.ExamResponse;
import de.studyplanner.task.TaskResponse;

import java.util.List;

/** Suchergebnis, getrennt nach Kursen, Klausuren und Aufgaben. */
public record SearchResponse(
        List<CourseResponse> courses,
        List<ExamResponse> exams,
        List<TaskResponse> tasks
) {

    /** Leeres Ergebnis (z. B. bei zu kurzem Suchtext). */
    public static SearchResponse empty() {
        return new SearchResponse(List.of(), List.of(), List.of());
    }
}
